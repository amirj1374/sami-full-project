<script setup lang="ts">
// Minimal centered shell for the login/register screens.
import LanguageSwitcher from '@/components/LanguageSwitcher.vue'
</script>

<template>
  <v-main class="auth-layout">
    <div class="auth-layout__glow auth-layout__glow--primary" />
    <div class="auth-layout__glow auth-layout__glow--accent" />

    <header class="auth-layout__header">
      <div class="auth-layout__brand">
        <v-avatar rounded="lg" color="primary" size="42" class="auth-layout__logo">
          <span class="text-h6 font-weight-bold">S</span>
        </v-avatar>
        <div>
          <div class="text-subtitle-1 font-weight-bold">{{ $t('common.appName') }}</div>
          <div class="text-caption text-medium-emphasis">{{ $t('shell.tagline') }}</div>
        </div>
      </div>
      <LanguageSwitcher />
    </header>

    <v-container class="auth-layout__container" fluid>
      <v-row justify="center" align="center" class="auth-layout__row">
        <v-col cols="12" sm="9" md="6" lg="5" xl="4" class="auth-layout__column">
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
  background:
    linear-gradient(145deg, rgba(var(--v-theme-primary), 0.08), transparent 48%),
    rgb(var(--v-theme-background));
}
.auth-layout::before {
  position: absolute;
  inset: 0;
  content: '';
  opacity: 0.35;
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
}
.auth-layout__logo {
  box-shadow: 0 8px 20px rgba(var(--v-theme-primary), 0.24);
}
.auth-layout__container {
  position: relative;
  z-index: 1;
  min-height: calc(100dvh - 72px);
  padding-block: 20px 48px;
}
.auth-layout__row {
  min-height: calc(100dvh - 140px);
}
.auth-layout__column {
  max-width: 520px;
}

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
