## 笔记

### 项目初始化

1、在github上创建项目
2、本地连接github，拉下来代码
3、新建```.gitignore```文件
4、打开终端，依次执行
```git
git add .gitignore
git commit -m "first commit"
git remote add origin https://github.com/xxx/xxx.git
git push -u origin master
```

如果出现不想要的文件添加到git，可以先执行以下命令，然后重新添加
```git
git rm --cached .
```

### 后端

出现循环依赖后，启动会报错，可以在注入时加上注解@Lazy

<img src="/pic/img_1.png">


### 前端

使用nvm 来管理node.js版本

这里提到了前端的渲染模式
客户端渲染(返回html页面加js脚本，然后浏览器执行脚本逐步完成渲染)
服务端渲染（由服务器端生成完整html）
静态渲染（博客）


快速构建前端工程（这里使用了next.js）
具体参考官方文档
https://nextjs.org/docs/app/getting-started/installation#automatic-installation
