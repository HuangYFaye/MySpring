package com.one.mvcframework.servlet;

import com.one.mvcframework.servlet.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by huangyifei on 2018/06/12.
 */
public class DispatcherServlet extends HttpServlet {

    //配置文件对象
    private Properties contextConfig = new Properties();
    //扫描出来的所有类名容器
    private List<String> classNames = new ArrayList<String>();
    //IOC容器
    private Map<String, Object> ioc = new HashMap<String, Object>();
    //用于存放检查URL的正则表达式、Controller对象、方法对象、方法参数集合
    private List<Handler> handlerMapping = new ArrayList<Handler>();


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //6。等待请求
        try {
            boolean isMatcher = pattern(req,resp);
            if (!isMatcher){
                resp.getWriter().write("404 Not Found!");
            }
        } catch (Exception e) {
            resp.getWriter().write("500 Exception,Details:\r\n" +
                    e.getMessage() + "\r\n" +
                    Arrays.toString(e.getStackTrace()).replaceAll("\\[\\]", "")
                            .replaceAll(",\\s", "\r\n"));
        }
    }

    private boolean pattern(HttpServletRequest req, HttpServletResponse resp) throws Exception{
        if (handlerMapping.isEmpty()){return false;};

        // /progectName/demo/test.json
        String url = req.getRequestURI();
        //  progectName
        String contextPath = req.getContextPath();
        // /progectName/demo/test.json
        url = url.replace(contextPath, "").replaceAll("/+","/");

        //从handlerMapping中去匹配URL，命中后检查handler里的paramMapping是否含有RequestParam里的参数，通过反射调起方法
        for (Handler handler : handlerMapping) {

            try {
                Matcher matcher = handler.pattern.matcher(url);
                if (!matcher.matches()) {continue;}
                //获取方法的参数类型数组
                Class<?>[] paramTypes = handler.method.getParameterTypes();
                Object[] paramValues = new Object[paramTypes.length];
                //获取请求里的参数
                Map<String, String[]> params = req.getParameterMap();

                for (Map.Entry<String, String[]> param : params.entrySet()) {
                    String value = Arrays.toString((param.getValue())).replaceAll("\\]|\\[", "").replaceAll(",\\s", ",");
                    if (!handler.paramMapping.containsKey(param.getKey())) {
                        continue;
                    }

                    int index = handler.paramMapping.get(param.getKey());
                    paramValues[index] = castStringValue(value, paramTypes[index]);
                }

                int reqIndex = handler.paramMapping.get(HttpServletRequest.class.getName());
                paramValues[reqIndex] = req;

                int repIndex = handler.paramMapping.get(HttpServletResponse.class.getName());
                paramValues[repIndex] = resp;

                handler.method.invoke(handler.controller, paramValues);

                return true;
            }catch (Exception e){
                throw e;
            }
        }
        return false;
    }

    private Object castStringValue(String value, Class<?> clazz) {
        if (clazz == String.class) {
            return  value;
        } else if(clazz == Integer.class){
            return Integer.valueOf(value);
        } else if (clazz == int.class) {
            return Integer.valueOf(value).intValue();
        } else {
            return null;
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        //1.加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));
        //2.扫描所相关联的类
        doScanner(contextConfig.getProperty("scanPackage"));
        //3.初始化类并将其保存到IOC容器之中
        doInstance();
        //4.实现自动DI（依赖注入）
        doAutowired();
        //5.建立URL和Method的映射关系(HandlerMapping)
        InitHandleMapping();
    }

    private void InitHandleMapping() {
        if (ioc.isEmpty()){return;}

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();

            if (!clazz.isAnnotationPresent(Controller.class)){continue;}

            String baseUrl = "";
            if (clazz.isAnnotationPresent(RequestMapping.class)) {
                RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
                baseUrl = requestMapping.value();
            }

            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(RequestMapping.class)){continue;}

                RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                String customRegex = ("/" + baseUrl + requestMapping.value()).replaceAll("/+","/");
                String regex = customRegex.replaceAll("\\*", ".*");

                Map<String, Integer> pm = new HashMap<String, Integer>();

                //获取方法上所有标签
                Annotation [] [] pa = method.getParameterAnnotations();
                for (int i = 0; i < pa.length; i++) {
                    for (Annotation a : pa[i]) {
                        if (a instanceof RequestParam) {
                            String parmaName = ((RequestParam) a).value();
                            if (!"".equals(parmaName.trim())) {
                                pm.put(parmaName,i);
                            }
                        }
                    }
                }

                //提取Request和Response的索引
                Class<?> [] paramsTypes = method.getParameterTypes();
                for (int i = 0; i < paramsTypes.length; i++) {
                    Class<?> type = paramsTypes[i];
                    if (type == HttpServletResponse.class||
                        type == HttpServletRequest.class) {
                        pm.put(type.getName(), i);
                    }
                }

                handlerMapping.add(new Handler(Pattern.compile(regex),entry.getValue(),method,pm));

                System.out.println("Mapping " + customRegex + "  " + method);
            }
        }

    }

    private void doAutowired() {
        if (ioc.isEmpty()){return;}

        for (Map.Entry<String,Object> entry : ioc.entrySet()) {
            //获取所有属性（包括私有属性）
            Field [] fields = entry.getValue().getClass().getDeclaredFields();

            for (Field field : fields) {
                //不是所有属性都要赋值
                if (field.isAnnotationPresent(Autowried.class)) {
                    Autowried autowried = field.getAnnotation(Autowried.class);
                    String beanName = autowried.value().trim();
                    if ("".equals(beanName)) {
                        beanName = field.getType().getName();
                    }
                    //设置field的属性为可操作（包括私有属性）
                    field.setAccessible(true);
                    try {
                        field.set(entry.getValue(),ioc.get(beanName));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                } else {
                    continue;
                }
            }
        }
    }

    private void doInstance() {
        if(classNames.isEmpty()){return;}

        try {
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);

                //初始化
                //不是所有类都要初始化
                if (clazz.isAnnotationPresent(Controller.class)){
                    //对带Controller注解、Service注解的类进行初始化，并保存到IOC容器中
                    //Key的规则
                    //1.默认类名首字母小写
                    //2.自定类名
                    //3.投机取巧，以接口全类名作为Key，以实现类的实例作为值保存下来

                    Controller controller = clazz.getAnnotation(Controller.class);
                    String controllerBeanName = controller.value();
                    putIOC(controllerBeanName,clazz);
                }else if (clazz.isAnnotationPresent(Service.class)){
                    Service service = clazz.getAnnotation(Service.class);
                    String serviceBeanName  = service.value();
                    putIOC(serviceBeanName,clazz);

                }else{
                    //无注解忽略
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doScanner(String scanPackage) {
        //将包名换成路径，再使用classLoader加载
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        //使用URL路径创建File对象，然后递归扫描文件，判断为非目录则装载到classNames List容器
        File classDir = new File(url.getFile());
        for (File file:classDir.listFiles()){
            if(file.isDirectory()){
                doScanner(scanPackage + "." + file.getName());
            }
            else{
                String testName = file.getName();
                String className = (scanPackage + "." + file.getName()).replace(".class", "");
                classNames.add(className);
            }
        }
    }

    private void doLoadConfig(String contextConfigLocation) {

        InputStream is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            contextConfig.load(is);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(null != is){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String lowerFirstCase(String str){

        char[] chars = str.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    private void putIOC(String beanName,Class<?> clazz){

        Object instance = null;
        try {
            if ("".equals(beanName.trim()))
            {
                beanName = lowerFirstCase(clazz.getSimpleName());
            }

            instance = clazz.newInstance();
            ioc.put(beanName,instance);

            //将类实现的接口都初始化放到IOC容器中
            Class<?>[] interfaces = clazz.getInterfaces();
            for (Class<?> i : interfaces) {
                ioc.put(i.getName(), instance);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //内部类，protected属性表明只能在同个包下访问
    private  class Handler{
        protected Pattern pattern;
        protected Object controller;
        protected Method method;
        protected Map<String,Integer> paramMapping;

        public Handler(Pattern pattern, Object controller, Method method, Map<String, Integer> paramMapping) {
            this.pattern = pattern;
            this.controller = controller;
            this.method = method;
            this.paramMapping = paramMapping;
        }
    }
}
