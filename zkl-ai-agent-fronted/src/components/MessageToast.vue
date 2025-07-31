<template>
  <div v-if="show" :class="['message-toast', `message-${type}`]" @click="hide">
    <div class="message-content">
      <div class="message-icon">
        <span v-if="type === 'success'">✓</span>
        <span v-else-if="type === 'error'">✗</span>
        <span v-else-if="type === 'warning'">⚠</span>
        <span v-else>ℹ</span>
      </div>
      <div class="message-text">{{ message }}</div>
      <div class="message-close" @click.stop="hide">×</div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'MessageToast',
  props: {
    message: {
      type: String,
      required: true
    },
    type: {
      type: String,
      default: 'info',
      validator: (value) => ['success', 'error', 'warning', 'info'].includes(value)
    },
    duration: {
      type: Number,
      default: 3000
    },
    show: {
      type: Boolean,
      default: false
    }
  },
  emits: ['hide'],
  watch: {
    show(newVal) {
      if (newVal && this.duration > 0) {
        setTimeout(() => {
          this.hide()
        }, this.duration)
      }
    }
  },
  methods: {
    hide() {
      this.$emit('hide')
    }
  }
}
</script>

<style scoped>
.message-toast {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  min-width: 300px;
  max-width: 500px;
  animation: slideDown 0.3s ease-out;
}

.message-content {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  background-color: var(--white);
  border-left: 4px solid;
  cursor: pointer;
}

.message-success .message-content {
  border-left-color: var(--secondary-color);
  background-color: #f0f9ff;
}

.message-error .message-content {
  border-left-color: var(--quaternary-color);
  background-color: #fef2f2;
}

.message-warning .message-content {
  border-left-color: var(--tertiary-color);
  background-color: #fffbeb;
}

.message-info .message-content {
  border-left-color: var(--primary-color);
  background-color: #f0f9ff;
}

.message-icon {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  font-weight: bold;
  font-size: 16px;
}

.message-success .message-icon {
  color: var(--secondary-color);
}

.message-error .message-icon {
  color: var(--quaternary-color);
}

.message-warning .message-icon {
  color: var(--tertiary-color);
}

.message-info .message-icon {
  color: var(--primary-color);
}

.message-text {
  flex: 1;
  font-size: 14px;
  line-height: 1.4;
  color: var(--text-color);
}

.message-close {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 12px;
  cursor: pointer;
  font-size: 18px;
  color: #999;
  transition: color 0.2s;
}

.message-close:hover {
  color: var(--text-color);
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
}

/* 响应式调整 */
@media (max-width: 768px) {
  .message-toast {
    left: 20px;
    right: 20px;
    transform: none;
    min-width: auto;
  }
}
</style>