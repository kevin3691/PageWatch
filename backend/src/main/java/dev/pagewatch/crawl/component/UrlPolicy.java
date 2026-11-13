package dev.pagewatch.crawl.component;

import dev.pagewatch.common.web.ApiException;

import org.springframework.stereotype.Component;
import java.net.*;
import java.util.Locale;

@Component
public class UrlPolicy {
    public URI validate(String value) {
        try {
            URI uri = URI.create(value.strip());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!(scheme.equals("http") || scheme.equals("https")) || uri.getHost() == null || uri.getUserInfo() != null)
                throw ApiException.badRequest("请输入完整的 http:// 或 https:// 网址，不支持带账号密码的网址");
            if (uri.getPort() != -1 && uri.getPort() != 80 && uri.getPort() != 443)
                throw ApiException.badRequest("目前仅支持使用 80 或 443 端口的公开网页");
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (isPrivate(address)) throw ApiException.badRequest("请使用公开网页地址，不能抓取本机、内网或保留地址");
            }
            return uri;
        } catch (UnknownHostException e) {
            throw ApiException.badRequest("无法解析这个网站的域名，请检查网址");
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("网址格式不正确");
        }
    }

    static boolean isPrivate(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) return true;
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int a = bytes[0] & 255, b = bytes[1] & 255, c = bytes[2] & 255;
            return a == 0 || a >= 224 || a == 127 || (a == 100 && b >= 64 && b <= 127)
                    || (a == 169 && b == 254) || (a == 192 && b == 0)
                    || (a == 192 && b == 168) || (a == 198 && (b == 18 || b == 19 || b == 51 && c == 100))
                    || (a == 203 && b == 0 && c == 113);
        }
        // Public IPv6 global unicast only; exclude documentation ranges.
        return (bytes[0] & 0xe0) != 0x20 || (bytes[0] == 0x20 && bytes[1] == 0x01 && (bytes[2] & 255) == 0x0d && (bytes[3] & 255) == 0xb8);
    }
}
