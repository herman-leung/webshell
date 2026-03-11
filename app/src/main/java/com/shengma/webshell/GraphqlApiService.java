package com.shengma.webshell;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;

import java.util.Map;

// GraphQL 接口定义
public interface GraphqlApiService {
//         * @param accept            对应 curl 的 accept 头
//     * @param acceptLanguage    对应 accept-language 头
//     * @param authorization     对应 authorization 头（Bearer token）
//            * @param cacheControl      对应 cache-control 头
//     * @param contentType       对应 content-type 头
//     * @param origin            对应 origin 头
//     * @param pragma            对应 pragma 头
//     * @param priority          对应 priority 头
//     * @param referer           对应 referer 头
//     * @param secChUa           对应 sec-ch-ua 头
//     * @param secChUaMobile     对应 sec-ch-ua-mobile 头
//     * @param secChUaPlatform   对应 sec-ch-ua-platform 头
//     * @param secFetchDest      对应 sec-fetch-dest 头
//     * @param secFetchMode      对应 sec-fetch-mode 头
//     * @param secFetchSite      对应 sec-fetch-site 头
    /**
     * 发送 GraphQL 请求

     * @param headers         对应 user-agent 头
     * @param requestBody       请求体（对应 --data-raw）
     * @return 响应结果
     */
    @POST("graphql") // 接口路径（baseUrl 之外的部分）
    Call<GraphqlResponse> sendGraphql(
            // 使用 HeaderMap 让调用方传一个头部集合，而不是列出几十个参数
            @HeaderMap Map<String, String> headers,
            @Body GraphqlRequest requestBody
    );

    // 保留老方法以兼容旧代码（如果还有其它地方调用），可以在以后删除
    @Deprecated
    @POST("graphql")
    Call<GraphqlResponse> getDeviceFirmware(
            @Header("accept") String accept,
            @Header("accept-language") String acceptLanguage,
            @Header("authorization") String authorization,
            @Header("cache-control") String cacheControl,
            @Header("content-type") String contentType,
            @Header("origin") String origin,
            @Header("pragma") String pragma,
            @Header("priority") String priority,
            @Header("referer") String referer,
            @Header("sec-ch-ua") String secChUa,
            @Header("sec-ch-ua-mobile") String secChUaMobile,
            @Header("sec-ch-ua-platform") String secChUaPlatform,
            @Header("sec-fetch-dest") String secFetchDest,
            @Header("sec-fetch-mode") String secFetchMode,
            @Header("sec-fetch-site") String secFetchSite,
            @Header("user-agent") String userAgent,
            @Body GraphqlRequest requestBody
    );
}