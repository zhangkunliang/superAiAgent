package com.zkl.zklimagesearchmcp.tools;

import cn.hutool.core.lang.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ImageSearchToolTest {

    @Resource
    private ImageSearchTool imageSearchTool;
    @Test
    void searchImage() {
    }

    @Test
    void searchMediumImages() {
        List<String> list = imageSearchTool.searchMediumImages("美女");
        Assert.notNull(list);
    }
}