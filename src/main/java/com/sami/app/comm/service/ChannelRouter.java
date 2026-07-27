package com.sami.app.comm.service;

import com.sami.app.calendar.api.WorkingTimeProvider;
import com.sami.app.comm.domain.CommChannel;
import com.sami.app.comm.domain.CommRoutingRule;
import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Picks the channel a message travels on.
 *
 * <p>Order of authority: an explicit caller choice wins; otherwise routing
 * rules are evaluated highest-priority-first and the first match decides. A
 * rule's preferred channel is used when it can send, its fallback when not —
 * the "provider unavailable → fallback activation" edge case as data.
 *
 * <p>Business hours: a matching rule with {@code respectBusinessHours} defers
 * the message (returns a deferral instant) unless the message's priority
 * reaches the rule's emergency override. Deferral is not rejection — the
 * message queues and the sweep releases it when the working window opens.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChannelRouter {

    private final WorkingTimeProvider workingTime;

    /** The routing outcome: a channel, and optionally "not before" timing. */
    public record Route(CommChannel channel, Instant notBefore, String ruleCode) { }

    /**
     * @param explicit  caller-chosen channel, or null to route by rule
     * @param rules     active rules, highest priority first
     * @param moduleCode caller module for rule scoping
     * @param priority  message priority
     */
    public Route route(CommChannel explicit, List<CommRoutingRule> rules,
                       String moduleCode, int priority, Instant now) {
        if (explicit != null) {
            requireSendable(explicit);
            return new Route(explicit, null, null);
        }

        for (CommRoutingRule rule : rules) {
            if (!rule.matches(moduleCode, priority)) {
                continue;
            }
            Optional<CommChannel> channel = firstSendable(rule);
            if (channel.isEmpty()) {
                // Preferred AND fallback both down: keep evaluating lower
                // rules rather than failing — a dead channel must not take
                // the whole hub down with it.
                log.warn("Routing rule '{}' matched but no channel of it can send", rule.getCode());
                continue;
            }
            Instant notBefore = deferUntil(rule, channel.get(), priority, now);
            return new Route(channel.get(), notBefore, rule.getCode());
        }

        throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                "No routing rule matched and no channel was specified — configure a default route");
    }

    private Optional<CommChannel> firstSendable(CommRoutingRule rule) {
        if (rule.getPreferredChannel() != null && rule.getPreferredChannel().canSend()) {
            return Optional.of(rule.getPreferredChannel());
        }
        if (rule.getFallbackChannel() != null && rule.getFallbackChannel().canSend()) {
            log.info("Routing rule '{}': preferred channel unavailable, using fallback '{}'",
                    rule.getCode(), rule.getFallbackChannel().getCode());
            return Optional.of(rule.getFallbackChannel());
        }
        return Optional.empty();
    }

    /**
     * When business hours apply and the clock is outside them, the release
     * instant is the start of the next bookable day (via the V23 calendar).
     * Failure to resolve a calendar falls open — deferring a message on a
     * broken calendar configuration would silently stop all communication.
     */
    private Instant deferUntil(CommRoutingRule rule, CommChannel channel, int priority, Instant now) {
        if (!rule.isRespectBusinessHours()) {
            return null;
        }
        if (rule.getEmergencyPriority() > 0 && priority >= rule.getEmergencyPriority()) {
            return null;
        }
        try {
            Long calendarId = workingTime.resolveCalendarId(channel.getCompanyId(), channel.getBranchId());
            if (workingTime.isWithinWorkingHours(calendarId, now, now.plusSeconds(60))) {
                return null;
            }
            var zone = java.time.ZoneId.systemDefault();
            var today = now.atZone(zone).toLocalDate();
            var day = workingTime.scheduleFor(calendarId, today);
            // Later today?
            var nowTime = now.atZone(zone).toLocalTime();
            var todayWindow = day.windows().stream()
                    .filter(w -> w.start().isAfter(nowTime))
                    .findFirst();
            if (day.bookable() && todayWindow.isPresent()) {
                return today.atTime(todayWindow.get().start()).atZone(zone).toInstant();
            }
            var nextDay = workingTime.nextWorkingDay(calendarId, today);
            var nextSchedule = workingTime.scheduleFor(calendarId, nextDay);
            var start = nextSchedule.windows().isEmpty()
                    ? java.time.LocalTime.of(9, 0)
                    : nextSchedule.windows().get(0).start();
            return nextDay.atTime(start).atZone(zone).toInstant();
        } catch (RuntimeException e) {
            log.warn("Business-hours deferral skipped (calendar unresolved): {}", e.getMessage());
            return null;
        }
    }

    private void requireSendable(CommChannel channel) {
        if (!channel.canSend()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Channel '%s' is %s and cannot send".formatted(channel.getCode(),
                            channel.getStatus() != null
                                    ? channel.getStatus().getName().toLowerCase()
                                    : "misconfigured"));
        }
    }
}
