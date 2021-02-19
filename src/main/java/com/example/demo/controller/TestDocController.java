package com.example.demo.controller;

import com.example.demo.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * CAI单cotroller
 */
@RestController
@RequestMapping("/rest/test")
public class TestDocController {

    @Autowired
    private TestService testService;

    @RequestMapping(value = "save", method = RequestMethod.GET)
    public void saveApi() {
        testService.saveApi();
    }

    @RequestMapping(value = "view", method = RequestMethod.GET)
    public Object viewApi(@RequestParam String key) {
        return testService.getRedis(key);
    }
}
