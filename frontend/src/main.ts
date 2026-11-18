import { createApp } from "vue";
import { createPinia } from "pinia";
import ElementPlus from "element-plus";
import zhCn from "element-plus/es/locale/lang/zh-cn";
import * as ElementPlusIconsVue from "@element-plus/icons-vue";
import "element-plus/dist/index.css";
import "./styles/index.css";

import App from "./App.vue";
import router from "./router";
import { useAuthStore } from "./stores/auth";

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);
app.use(router);
app.use(ElementPlus, { locale: zhCn });

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component);
}

/**
 * 按钮级权限指令：v-perm="'system:user:create'" 或 v-perm="['a','b']"
 */
app.directive("perm", {
  mounted(el: HTMLElement, binding) {
    const auth = useAuthStore();
    const codes: string[] = Array.isArray(binding.value)
      ? binding.value
      : [binding.value];
    const allowed =
      auth.isSuperAdmin ||
      codes.some((code) => auth.permissions.includes(code));
    if (!allowed) {
      el.parentNode?.removeChild(el);
    }
  },
});

app.mount("#app");
