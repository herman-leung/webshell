package com.shengma.webshell;

import java.util.List;

// 2. GraphQL 响应体（匹配接口返回的 JSON 结构）
public class GraphqlResponse {
    // 注意：服务器返回的 JSON 包裹在 "data" 字段中，所以这里需要额外一层
    private Data data;

    // Getter（Retrofit 解析需要）
    public Data getData() {
        return data;
    }

    // 外层 data 封装类
    public static class Data {
        private Result result; // 接口返回的核心数据

        public Result getResult() {
            return result;
        }
    }

    // 内部类：result 字段
    public static class Result {
        private List<DeviceFirmware> nodes; // 固件列表
        private PageInfo pageInfo;          // 分页信息

        // Getter
        public List<DeviceFirmware> getNodes() {
            return nodes;
        }
        public PageInfo getPageInfo() {
            return pageInfo;
        }
    }

    // 内部类：固件信息（nodes 里的每一项）
    public static class DeviceFirmware {
        // 响应中 id 是 UUID 字符串，改为 String 才能正确反序列化
        private String id;
        private String name;
        private String fileName;
        private String verify;
        private String fileUrl;
        private String versionName;
        private int type;
        private String desc;

        // Getter（按需添加，比如获取 name、fileUrl 等）
        public String getId() {
            return id;
        }
        public String getName() {
            return name;
        }
        public String getFileName() {
            return fileName;
        }
        public String getVerify() {
            return verify;
        }
        public String getFileUrl() {
            return fileUrl;
        }
        public String getVersionName() {
            return versionName;
        }
        public int getType() {
            return type;
        }
        public String getDesc() {
            return desc;
        }
    }

    // 内部类：分页信息（pageInfo）
    public static class PageInfo {
        private int totalCount;
        private int pageSize;
        private int currentPage;

        // Getter
        public int getTotalCount() {
            return totalCount;
        }

        // 下面两个字段在原代码里也有，之前缺少访问方法，请求里也会返回它们
        public int getPageSize() {
            return pageSize;
        }

        public int getCurrentPage() {
            return currentPage;
        }
    }
}