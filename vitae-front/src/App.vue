<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<template>
  <RouterView />
</template>

<style>
/* El contenedor raíz se adapta al 100% de la ventana sin márgenes restrictivos */
#app {
  width: 100%;
  min-height: 100vh;
  margin: 0;
  padding: 0;
}
</style>
