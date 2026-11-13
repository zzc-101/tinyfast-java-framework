import { createApp } from 'vue';
import { createPinia } from 'pinia';
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate';
import 'ant-design-vue/dist/reset.css';
import App from './App.vue';
import router from './router';
import { permission } from './directives/permission';

const app = createApp(App);
const pinia = createPinia();
pinia.use(piniaPluginPersistedstate);

app.use(pinia);
app.use(router);
app.directive('permission', permission);
app.mount('#app');
