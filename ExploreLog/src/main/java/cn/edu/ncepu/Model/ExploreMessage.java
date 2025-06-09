package cn.edu.ncepu.Model;

import com.google.gson.Gson;

public class ExploreMessage {
    private String msgType;
    private String msgContent;

    /**
     * 将json转为一个ExploreMessage对象
     *
     * @param json json字符串
     * @return 一个ExploreMessage对象
     */
    public static ExploreMessage getExploreMessage(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, ExploreMessage.class);
    }

    /**
     * 将自身转为json字符串
     *
     * @return 对应的json
     */
    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public ExploreMessage(String msgType, String msgContent) {
        this.msgType = msgType;
        this.msgContent = msgContent;
    }

    public String getMsgType() {
        return msgType;
    }

    public String getMsgContent() {
        return msgContent;
    }
}
