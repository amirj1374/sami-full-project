package com.sami.app.automation;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.assertThat;
class AutomationNotificationIntegrationContractTest {
 @Test void notifyActionUsesNotificationCenterNotDirectBusinessDelivery() throws Exception {String s=Files.readString(Path.of("src/main/java/com/sami/app/automation/provider/NotificationActionProvider.java"));assertThat(s).contains("StaffNotificationService").contains("createSystem").contains("notification-center").doesNotContain("RestClient").doesNotContain("JavaMailSender");}
}
