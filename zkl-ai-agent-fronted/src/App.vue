<template>
  <div class="app">
    <router-view />
    <footer class="footer">
      <div class="container">
        <p>&copy; {{ currentYear }} 天王星数字科技有限公司 保留所有权利</p>
      </div>
    </footer>
  </div>
</template>

<script>
export default {
  name: 'App',
  computed: {
    currentYear() {
      return new Date().getFullYear();
    }
  }
}
</script>

<style>
.app {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.footer {
  margin-top: auto;
  padding: 15px 0;
  background-color: var(--white);
  border-top: 1px solid var(--light-gray);
  text-align: center;
  font-size: 14px;
  color: #666;
}
</style> 