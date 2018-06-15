package com.one.demo.service.impl;

import com.one.demo.service.IDemoService;
import com.one.mvcframework.servlet.annotation.Service;

/**
 * Created by huangyifei on 2018/6/12.
 */
@Service
public class DemoService implements IDemoService {
    @Override
    public String get(String name) {
        return "My name is" + name;
    }
}
