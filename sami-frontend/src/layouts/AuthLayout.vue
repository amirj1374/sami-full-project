<script setup lang="ts">
// Minimal centered shell for the login/register screens.
import LanguageSwitcher from '@/components/LanguageSwitcher.vue'
</script>

<template>
  <v-main>
    <v-app-bar flat color="transparent">
      <v-spacer />
      <LanguageSwitcher />
    </v-app-bar>
    <v-container class="fill-height" fluid>
      <v-row justify="center" align="center">
        <v-col cols="12" sm="8" md="5" lg="4">
          <router-view />
        </v-col>
      </v-row>
    </v-container>
  </v-main>
</template>
