from pathlib import Path
import re, sys
root = Path(sys.argv[1])
gradle = root / "app/build.gradle.kts"
s = gradle.read_text()
s = s.replace('versionCode = 2', 'versionCode = 3')
s = s.replace('versionName = "1.1.0-auto-mac-portal-detect"', 'versionName = "1.2.0-live-player-stability"')
gradle.write_text(s)

stalker = root / "app/src/main/java/com/xili/macstb/net/StalkerClient.java"
s = stalker.read_text()
new_method = r'''    public String createLink(String cmd) throws Exception {
        if (cmd == null || cmd.trim().isEmpty()) throw new Exception("Empty stream command");
        String clean = cmd.trim();
        String directFallback = stripPlayerPrefix(clean);
        Exception createError = null;

        try {
            String q = "type=itv&action=create_link&cmd=" + enc(clean)
                    + "&series=0&forced_storage=0&disable_ad=0&download=0&JsHttpRequest=1-xml";
            Object js = request(q);
            String result = "";
            if (js instanceof JSONObject) {
                JSONObject o = (JSONObject) js;
                result = first(o, "cmd", "url");
                if (result.isEmpty() && o.optJSONObject("data") != null) {
                    result = first(o.optJSONObject("data"), "cmd", "url");
                }
            }
            result = stripPlayerPrefix(result);
            if (!result.isEmpty()) return result;
        } catch (Exception e) {
            createError = e;
        }

        if (directFallback.startsWith("http://") || directFallback.startsWith("https://")) {
            return directFallback;
        }
        if (createError != null) throw createError;
        throw new Exception("Portal did not create a stream link");
    }

'''
pattern = r'    public String createLink\(String cmd\) throws Exception \{.*?\n    \}\n\n(?=    public JSONObject getAccountInfo\(\) throws Exception)'
s2, n = re.subn(pattern, new_method, s, flags=re.S)
if n != 1:
    raise SystemExit(f"createLink replacement failed: {n}")
stalker.write_text(s2)
