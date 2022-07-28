package org.ccs.m3u8sync;

import lombok.extern.slf4j.Slf4j;
import org.ccs.m3u8sync.downup.service.DownUpService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles(value = "test")
@AutoConfigureMockMvc
@WebAppConfiguration
@Slf4j
class DownUpControllerMock {
    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();//建议使用这种
    }

    @Test
    void addAsync() throws Exception {
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post("/downup/addAsync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("roomId", DownUpService.CHECK_RELAY)
                        .param("m3u8Url", "test")
                        .content("{}")
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn();

        int status = mvcResult.getResponse().getStatus();                 //得到返回代码
        String content = mvcResult.getResponse().getContentAsString();    //得到返回结果
        log.info("---addAsync status={} content={}", status, content);
        Assertions.assertEquals(200, status, "/downup/addAsync");
    }

    @Test
    void callbackDel() throws Exception {
        String url="/downup/callbackDel/"+DownUpService.CHECK_CALLBACK;
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("roomId", "checkCallbackDel")
                        .param("successDel", "false")
                        .content("{}")
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn();
        int status = mvcResult.getResponse().getStatus();                 //得到返回代码
        String content = mvcResult.getResponse().getContentAsString();    //得到返回结果
        log.info("---callback status={} content={}", status, content);
        Assertions.assertEquals(200, status, "/downup/callback");
    }

    @Test
    void recover() throws Exception {
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/downup/recover")
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn();
        int status = mvcResult.getResponse().getStatus();                 //得到返回代码
        String content = mvcResult.getResponse().getContentAsString();    //得到返回结果
        log.info("---recover status={} content={}", status, content);
        Assertions.assertEquals(200, status, "/downup/recover");

    }

    @Test
    void remove() throws Exception {
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/downup/remove")
                        .param("roomId", "checkRemove")
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn();
        int status = mvcResult.getResponse().getStatus();                 //得到返回代码
        String content = mvcResult.getResponse().getContentAsString();    //得到返回结果
        log.info("---remove status={} content={}", status, content);
        Assertions.assertEquals(200, status, "/downup/remove");

    }

    @Test
    void status_all() throws Exception {
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/downup/status")
                        .param("type", "all")
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn();
        int status = mvcResult.getResponse().getStatus();                 //得到返回代码
        String content = mvcResult.getResponse().getContentAsString();    //得到返回结果
        log.info("---status status={} content={}", status, content);
        Assertions.assertEquals(200, status, "/downup/status");

    }

    @Test
    void status_config() throws Exception {
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/downup/status")
                        .param("type", "config")
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn();
        int status = mvcResult.getResponse().getStatus();                 //得到返回代码
        String content = mvcResult.getResponse().getContentAsString();    //得到返回结果
        log.info("---status status={} content={}", status, content);
        Assertions.assertEquals(200, status, "/downup/status");

    }
}
