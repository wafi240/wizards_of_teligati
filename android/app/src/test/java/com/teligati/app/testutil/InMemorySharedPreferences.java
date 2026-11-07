package com.teligati.app.testutil;

import android.content.SharedPreferences;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class InMemorySharedPreferences implements SharedPreferences {
    private final Map<String, Object> data = new HashMap<>();

    @Override
    public Map<String, ?> getAll() {
        return new HashMap<>(data);
    }

    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        Object val = data.get(key);
        return val instanceof String ? (String) val : defValue;
    }

    @Nullable
    @Override
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        Object val = data.get(key);
        return val instanceof Set ? (Set<String>) val : defValues;
    }

    @Override
    public int getInt(String key, int defValue) {
        Object val = data.get(key);
        return val instanceof Integer ? (Integer) val : defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
        Object val = data.get(key);
        return val instanceof Long ? (Long) val : defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
        Object val = data.get(key);
        return val instanceof Float ? (Float) val : defValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        Object val = data.get(key);
        return val instanceof Boolean ? (Boolean) val : defValue;
    }

    @Override
    public boolean contains(String key) {
        return data.containsKey(key);
    }

    @Override
    public Editor edit() {
        return new EditorImpl();
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

    public class EditorImpl implements Editor {
        private final Map<String, Object> temp = new HashMap<>();
        private boolean clear = false;

        @Override
        public Editor putString(String key, @Nullable String value) {
            temp.put(key, value);
            return this;
        }

        @Override
        public Editor putStringSet(String key, @Nullable Set<String> values) {
            temp.put(key, values);
            return this;
        }

        @Override
        public Editor putInt(String key, int value) {
            temp.put(key, value);
            return this;
        }

        @Override
        public Editor putLong(String key, long value) {
            temp.put(key, value);
            return this;
        }

        @Override
        public Editor putFloat(String key, float value) {
            temp.put(key, value);
            return this;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            temp.put(key, value);
            return this;
        }

        @Override
        public Editor remove(String key) {
            temp.put(key, this);
            return this;
        }

        @Override
        public Editor clear() {
            clear = true;
            return this;
        }

        @Override
        public boolean commit() {
            apply();
            return true;
        }

        @Override
        public void apply() {
            synchronized (InMemorySharedPreferences.this) {
                if (clear) {
                    data.clear();
                }
                for (Map.Entry<String, Object> entry : temp.entrySet()) {
                    if (entry.getValue() == this) {
                        data.remove(entry.getKey());
                    } else {
                        data.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
    }
}
