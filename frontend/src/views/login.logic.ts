type LoginForm = {
  username: string;
  password: string;
};

export function createDefaultLoginForm(): LoginForm {
  return {
    username: "admin",
    password: "123456",
  };
}

export function validateLoginForm(form: LoginForm): string | null {
  if (!form.username || !form.password) {
    return "请输入账号和密码";
  }
  return null;
}
