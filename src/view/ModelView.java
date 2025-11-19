package view;

import java.util.HashMap;
import java.util.Map;

public class ModelView {
    private String view;
    private Map<String, Object> data = new HashMap<>();

    public ModelView(String view) {
        this.view = view;
    }
    public ModelView() {
    }
    public String getView() {
        return view;
    }
    public void setView(String view) {
        this.view = view;
    }
    public Map<String, Object> getData() {
        return data;
    }
    public void setData(Map<String, Object> data) {
        this.data = data;
    }
    
    public void addItem(String key, Object value) {
        this.data.put(key, value);
    }
}
