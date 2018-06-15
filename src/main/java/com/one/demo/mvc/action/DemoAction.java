package com.one.demo.mvc.action;

import com.one.demo.service.IDemoService;
import com.one.mvcframework.servlet.annotation.Autowried;
import com.one.mvcframework.servlet.annotation.Controller;
import com.one.mvcframework.servlet.annotation.RequestMapping;
import com.one.mvcframework.servlet.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by huangyifei on 2018/6/12.
 */
@Controller
@RequestMapping("/demo")
public class DemoAction {

    @Autowried
    private IDemoService demoService;

    @RequestMapping("/query.json")
    public void query(HttpServletRequest req, HttpServletResponse resp,@RequestParam("name") String name){

        String result = demoService.get(name);

        try {
            resp.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @RequestMapping("/add.json")
    public void add(HttpServletRequest req, HttpServletResponse resp,@RequestParam("a") Integer a,@RequestParam("b") Integer b){

        try {
            resp.getWriter().write(a + "+" + b + "+" + "=" + (a + b));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
