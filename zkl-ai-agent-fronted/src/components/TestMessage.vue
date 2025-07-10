<template>
  <div class="test-container">
    <h2>换行符测试</h2>
    
    <div class="test-section">
      <h3>测试内容</h3>
      <div class="test-content">
        <chat-message
          role="assistant"
          :content="testContent"
          :timestamp="new Date().toISOString()"
          ai-name="测试AI"
          ai-avatar="/src/assets/ai-avatar-manus.svg"
        />
      </div>
    </div>
    
    <div class="test-section">
      <h3>原始内容</h3>
      <pre class="raw-content">{{ testContent }}</pre>
    </div>
    
    <div class="test-section">
      <h3>修改测试内容</h3>
      <textarea 
        v-model="testContent" 
        class="test-input"
        rows="10"
        placeholder="输入包含\n换行符的文本进行测试"
      ></textarea>
    </div>
  </div>
</template>

<script>
import ChatMessage from './ChatMessage.vue';

export default {
  name: 'TestMessage',
  components: {
    ChatMessage
  },
  data() {
    return {
      testContent: `Step 1: 工具 askZhipuAI 返回的结果："疾控监督员，即疾病预防控制监督员，是负责疾病预防控制工作监督的专业人员。他们的职责主要包括以下几个方面：\\n\\n1. 政策法规执行监督：监督疾病预防控制法律法规、政策、标准的执行情况，确保各项措施得到有效落实。\\n\\n2. 传染病防控监督：对传染病疫情报告、监测、预警、处置等环节进行监督，确保及时发现、报告、调查、控制传染病疫情。\\n\\n3. 公共卫生监督：对公共卫生事件、突发公共卫生事件的预防和应对工作进行监督，保障公众健康安全。\\n\\n4. 健康危害因素监督：对可能导致健康危害的因素（如环境污染、职业危害等）进行监督，防止健康危害的发生。\\n\\n5. 免疫规划监督：监督国家免疫规划的实施，确保疫苗的接种率，预防疫苗可预防的传染病。\\n\\n6. 健康促进监督：监督健康教育和健康促进活动，提高公众的健康意识和自我保健能力。\\n\\n7. 监督指导：对下级疾控机构、医疗机构、社区卫生服务中心等单位的疾病预防控制工作进行业务指导和监督。\\n\\n8. 信息收集与分析：收集疾病预防控制相关信息，进行数据分析，为政策制定和决策提供依据。\\n\\n9. 应急处理：在公共卫生事件发生时，参与应急处理工作，协助做好疫情的监测、分析、报告和处置。\\n\\n10. 培训与交流：组织或参与疾病预防控制相关人员的培训，提高其业务水平；与其他部门、机构进行交流合作。\\n\\n疾控监督员的工作对于维护公共卫生安全、保障人民群众身体健康具有重要意义。"Step 2: 思考完成 - 无需行动Step 3: 工具 doTerminate 返回的结果："任务结束"`
    }
  }
}
</script>

<style scoped>
.test-container {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.test-section {
  margin-bottom: 30px;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 15px;
}

.test-section h3 {
  margin-top: 0;
  margin-bottom: 15px;
  color: #333;
}

.raw-content {
  background-color: #f5f5f5;
  padding: 10px;
  border-radius: 4px;
  white-space: pre-wrap;
  font-family: monospace;
  font-size: 14px;
}

.test-input {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-family: monospace;
  font-size: 14px;
  resize: vertical;
}

.test-content {
  background-color: #f9f9f9;
  padding: 15px;
  border-radius: 4px;
}
</style>
