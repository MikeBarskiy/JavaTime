package com.itmo.controller;

import com.itmo.model.Code;
import com.itmo.service.ByteCodeLoader;
import com.itmo.service.CodeService;
import com.itmo.service.InMemoryJavaCompiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class CodeController {

    @Autowired
    private CodeService codeService;

    @MessageMapping("/send")
    @SendTo("/topic/transfer")
    public Code userCode(Code userCode) throws Exception {
        return new Code();
    }

    @MessageMapping("/resultSingle")
    @SendTo("/topic/checkcode")
    public List<Boolean> checkSingleCode(Code userCode) {
        String className = "UserClass";

        StringBuilder code = new StringBuilder();
        code.append(userCode.getCode());

        Class userClass = null;
        Object userObject = null;
        try {
            byte[] byteCode = InMemoryJavaCompiler.compile(className, code.toString());
            userClass = ByteCodeLoader.load(className, byteCode);
            userObject = userClass.newInstance();
        } catch (RuntimeException e) {
            handleRuntimeException(e);
        } catch (ClassNotFoundException e) {
            handleClassNotFoundException(e);
        } catch (IllegalAccessException e) {
            handleIllegalAccessException(e);
        } catch (InstantiationException e) {
            handleInstantiationException(e);
        }

        if (userClass == null) {
            throw new RuntimeException("Can't create Java Class with code: " + code.toString());
        }

        Method userMethod = findUserMethod(userClass);

        if (userMethod == null) {
            throw new RuntimeException("Method «UserCode» was not found");
        }

        return checkUserCode(userMethod, userObject, 2);
    }

    @MessageMapping("/result")
    @SendTo("/topic/checkcode")
    public List<Boolean> checkFullCode(Code userCode) {
        String className = "UserClass";

        StringBuilder code = new StringBuilder();
        code.append(userCode.getCode());

        Class userClass = null;
        Object userObject = null;
        try {
            byte[] byteCode = InMemoryJavaCompiler.compile(className, code.toString());
            userClass = ByteCodeLoader.load(className, byteCode);
            userObject = userClass.newInstance();
        } catch (RuntimeException e) {
            handleRuntimeException(e);
        } catch (ClassNotFoundException e) {
            handleClassNotFoundException(e);
        } catch (IllegalAccessException e) {
            handleIllegalAccessException(e);
        } catch (InstantiationException e) {
            handleInstantiationException(e);
        }

        if (userClass == null) {
            throw new RuntimeException("Can't create Java Class with code: " + code.toString());
        }

        Method userMethod = findUserMethod(userClass);

        if (userMethod == null) {
            throw new RuntimeException("Method «UserCode» was not found");
        }

        return checkUserCode(userMethod, userObject, 1);
    }

    private Method findUserMethod(Class userClass) {
        Method[] methods = userClass.getMethods();
        Method userMethod = null;

        for (Method method : methods) {
            if (method.getName().equals("result")) {
                userMethod = method;
                break;
            }
        }
        return userMethod;
    }

    private List<Boolean> checkUserCode(Method userMethod, Object userObject, int taskId) {
        List<Boolean> resultList = new ArrayList<>();
        //TODO Add level and id of test
        Map<String, String> testsMap = codeService.findTests("elementary", taskId);

        for (Map.Entry<String, String> entry : testsMap.entrySet()) {
            try {
                if (userMethod.invoke(userObject, Integer.parseInt(entry.getKey())).toString()
                        .equals(entry.getValue())) {
                    resultList.add(true);
                } else
                    resultList.add(false);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        return resultList;
    }

    @MessageExceptionHandler
    @SendTo("/topic/error")
    public String handleClassNotFoundException(ClassNotFoundException ex) {
        return ex.getMessage();
    }

    @MessageExceptionHandler
    @SendTo("/topic/error")
    public String handleIllegalAccessException(IllegalAccessException ex) {
        return ex.getMessage();
    }

    @MessageExceptionHandler
    @SendTo("/topic/error")
    public String handleInstantiationException(InstantiationException ex) {
        return ex.getMessage();
    }

    @MessageExceptionHandler
    @SendTo("/topic/error")
    //TODO add more text about error
    public String handleRuntimeException(RuntimeException ex) {
        return ex.getMessage();
    }
}