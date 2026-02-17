package com.live.commerce.mall.controller

import cn.dev33.satoken.stp.StpUtil
import com.fasterxml.jackson.databind.ObjectMapper
import com.live.commerce.common.exception.ErrorCode
import com.live.commerce.mall.TestcontainersConfig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductMediaControllerIntegrationTest : TestcontainersConfig() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private var token: String = ""

    @BeforeEach
    fun setUp() {
        StpUtil.login(1L)
        token = StpUtil.getTokenValue()
    }

    @Test
    fun `should upload and fetch product media`() {
        val file = MockMultipartFile(
            "file",
            "product.png",
            "image/png",
            "mock-image-content".toByteArray()
        )

        val uploadResult = mockMvc.perform(
            multipart("/api/product/media/upload")
                .file(file)
                .header("satoken", token)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").isNumber)
            .andExpect(jsonPath("$.data.url").isNotEmpty)
            .andReturn()

        val mediaId = objectMapper.readTree(uploadResult.response.contentAsString)["data"]["id"].asLong()

        mockMvc.perform(get("/api/product/media/public/$mediaId"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "image/png"))
            .andExpect(content().bytes("mock-image-content".toByteArray()))
    }

    @Test
    fun `should reject empty product media upload`() {
        val emptyFile = MockMultipartFile("file", "empty.png", "image/png", ByteArray(0))

        mockMvc.perform(
            multipart("/api/product/media/upload")
                .file(emptyFile)
                .header("satoken", token)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_ERROR))
    }
}
