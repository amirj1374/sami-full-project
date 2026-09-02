<script setup lang="ts">
// Minimal centered shell for the login/register screens.
import LanguageSwitcher from '@/components/LanguageSwitcher.vue'
import { useThemeMode } from '@/composables/useThemeMode'

const { isDark, apply } = useThemeMode()
</script>

<template>
  <v-main class="auth-layout">
    <div class="auth-layout__glow auth-layout__glow--primary" />
    <div class="auth-layout__glow auth-layout__glow--accent" />

    <header class="auth-layout__header">
      <router-link
        :to="{ name: 'dashboard' }"
        class="auth-layout__brand"
        :aria-label="$t('dashboard.title')"
      >
        <v-avatar rounded="lg" color="primary" size="42" class="auth-layout__logo">
          <span class="text-h6 font-weight-bold">S</span>
        </v-avatar>
        <div>
          <div class="text-subtitle-1 font-weight-bold">{{ $t('common.appName') }}</div>
          <div class="text-caption text-medium-emphasis">{{ $t('shell.tagline') }}</div>
        </div>
      </router-link>
      <div class="d-flex align-center ga-1">
        <v-btn
          :icon="isDark ? 'mdi-weather-night' : 'mdi-white-balance-sunny'"
          variant="text"
          :aria-label="$t('shell.theme')"
          @click="apply(isDark ? 'light' : 'dark')"
        />
        <LanguageSwitcher />
      </div>
    </header>

    <v-container class="auth-layout__container" fluid>
      <v-row justify="center" align="center" class="auth-layout__row">
        <v-col cols="12" lg="7" xl="6" class="d-none d-lg-flex">
          <section class="auth-story" aria-labelledby="auth-story-title">
            <div class="auth-story__eyebrow">{{ $t('authExperience.kicker') }}</div>
            <h1 id="auth-story-title" class="auth-story__title">{{ $t('authExperience.title') }}</h1>
            <p class="auth-story__description">{{ $t('authExperience.description') }}</p>

            <div class="auth-story__features">
              <div v-for="feature in ['operations', 'insights', 'security']" :key="feature" class="auth-story__feature">
                <span class="auth-story__feature-icon">
                  <v-icon :icon="feature === 'operations' ? 'mdi-view-dashboard-outline' : feature === 'insights' ? 'mdi-chart-box-outline' : 'mdi-shield-check-outline'" />
                </span>
                <div>
                  <strong>{{ $t(`authExperience.${feature}.title`) }}</strong>
                  <p>{{ $t(`authExperience.${feature}.description`) }}</p>
                </div>
              </div>
            </div>

            <div class="auth-story__preview" aria-hidden="true">
              <div class="auth-story__preview-top">
                <span /><span /><span />
              </div>
              <div class="auth-story__preview-grid">
                <div class="auth-story__preview-nav" />
                <div class="auth-story__preview-body">
                  <div class="auth-story__preview-kpis"><i /><i /><i /></div>
                  <div class="auth-story__preview-chart" />
                </div>
              </div>
            </div>
          </section>
        </v-col>
        <v-col cols="12" sm="9" md="7" lg="5" xl="4" class="auth-layout__column">
          <router-view />
        </v-col>
      </v-row>
    </v-container>
  </v-main>
</template>

<style scoped>
.auth-layout {
  position: relative;
  min-height: 100dvh;
  overflow: hidden;
  background: rgb(var(--v-theme-background));
}
.auth-layout::before {
  position: absolute;
  inset: 0;
  content: '';
  opacity: 0.22;
  pointer-events: none;
  background-image: radial-gradient(rgba(var(--v-theme-primary), 0.15) 0.7px, transparent 0.7px);
  background-size: 22px 22px;
}
.auth-layout__glow {
  position: absolute;
  width: 360px;
  height: 360px;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.13;
  pointer-events: none;
}
.auth-layout__glow--primary {
  inset-block-start: -180px;
  inset-inline-start: -130px;
  background: rgb(var(--v-theme-primary));
}
.auth-layout__glow--accent {
  inset-block-end: -190px;
  inset-inline-end: -130px;
  background: rgb(var(--v-theme-accent));
}
.auth-layout__header {
  position: relative;
  z-index: 1;
  min-height: 72px;
  padding: 14px clamp(16px, 4vw, 48px);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.auth-layout__brand {
  display: flex;
  align-items: center;
  gap: 12px;
  border-radius: var(--app-radius-sm);
  color: inherit;
  text-decoration: none;
}
.auth-layout__brand:focus-visible {
  outline: 2px solid rgb(var(--v-theme-primary));
  outline-offset: 4px;
}
.auth-layout__logo {
  box-shadow: 0 8px 20px rgba(var(--v-theme-primary), 0.24);
}
.auth-layout__container {
  position: relative;
  z-index: 1;
  min-height: calc(100dvh - 72px);
  width: min(100%, 1480px);
  padding: 32px clamp(20px, 4vw, 64px) 56px;
}
.auth-layout__row {
  min-height: calc(100dvh - 140px);
}
.auth-layout__column {
  max-width: 520px;
}
.auth-story {
  width: min(100%, 660px);
  padding-inline-end: clamp(36px, 6vw, 96px);
}
.auth-story__eyebrow {
  display: inline-flex;
  padding: 7px 11px;
  border: 1px solid rgba(var(--v-theme-primary), 0.15);
  border-radius: 999px;
  color: rgb(var(--v-theme-primary));
  background: rgba(var(--v-theme-primary), 0.06);
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.04em;
}
.auth-story__title {
  max-width: 610px;
  margin-top: 20px;
  font-size: clamp(2.35rem, 4vw, 4.35rem);
  font-weight: 780;
  line-height: 1.12;
  letter-spacing: -0.045em;
}
.auth-story__description {
  max-width: 560px;
  margin-top: 20px;
  color: rgb(var(--v-theme-on-surface-variant));
  font-size: 1.02rem;
  line-height: 1.8;
}
.auth-story__features {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 30px;
}
.auth-story__feature {
  padding: 16px;
  border: 1px solid rgba(var(--v-border-color), 0.12);
  border-radius: var(--app-radius-lg);
  background: rgba(var(--v-theme-surface), 0.72);
}
.auth-story__feature-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  margin-bottom: 12px;
  border-radius: 10px;
  color: rgb(var(--v-theme-primary));
  background: rgba(var(--v-theme-primary), 0.09);
}
.auth-story__feature strong { font-size: 0.86rem; }
.auth-story__feature p {
  margin: 5px 0 0;
  color: rgb(var(--v-theme-on-surface-variant));
  font-size: 0.74rem;
  line-height: 1.55;
}
.auth-story__preview {
  margin-top: 26px;
  overflow: hidden;
  border: 1px solid rgba(var(--v-border-color), 0.14);
  border-radius: 22px;
  background: rgb(var(--v-theme-surface));
  box-shadow: var(--app-shadow-lg);
  transform: perspective(900px) rotateX(2deg) rotateY(-2deg);
}
[dir='rtl'] .auth-story__preview { transform: perspective(900px) rotateX(2deg) rotateY(2deg); }
.auth-story__preview-top { display: flex; gap: 6px; padding: 12px 14px; border-bottom: 1px solid var(--app-border); }
.auth-story__preview-top span { width: 7px; height: 7px; border-radius: 50%; background: rgba(var(--v-theme-on-surface), 0.18); }
.auth-story__preview-grid { display: grid; grid-template-columns: 88px 1fr; min-height: 190px; }
.auth-story__preview-nav { background: var(--app-nav-bg); }
.auth-story__preview-body { padding: 18px; }
.auth-story__preview-kpis { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.auth-story__preview-kpis i { display: block; height: 46px; border-radius: 10px; background: rgb(var(--v-theme-surface-container)); }
.auth-story__preview-chart { height: 86px; margin-top: 12px; border-radius: 12px; background: linear-gradient(180deg, rgba(var(--v-theme-primary), 0.1), rgba(var(--v-theme-primary), 0.02)); }

@media (max-width: 599px) {
  .auth-layout {
    overflow-y: auto;
  }
  .auth-layout__header {
    min-height: 76px;
    padding: max(12px, env(safe-area-inset-top)) 16px 10px;
  }
  .auth-layout__container {
    min-height: calc(100dvh - 76px);
    padding: 18px 0 0;
    display: flex;
    align-items: flex-end;
  }
  .auth-layout__row {
    width: 100%;
    min-height: auto;
    margin: 0;
    align-items: flex-end !important;
  }
  .auth-layout__column {
    max-width: none;
    padding: 0;
  }
}
</style>
