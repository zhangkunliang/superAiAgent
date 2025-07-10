import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import InspectorApp from '../views/InspectorApp.vue'
import ManusApp from '../views/ManusApp.vue'
import TestMessage from '../components/TestMessage.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: Home
    },
    {
      path: '/inspector-app/:chatId?',
      name: 'inspector-app',
      component: InspectorApp
    },
    {
      path: '/manus-app/:chatId?',
      name: 'manus-app',
      component: ManusApp
    },
    {
      path: '/test',
      name: 'test',
      component: TestMessage
    }
  ]
})

export default router 