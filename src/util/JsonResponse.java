package util;

public class JsonResponse {
    public String status;
    public String code;
    public int count;
    public Object data;

    public JsonResponse(String status, String code, int count, Object data) {
        this.status = status;
        this.code = code;
        this.count = count;
        this.data = data;
    }
}