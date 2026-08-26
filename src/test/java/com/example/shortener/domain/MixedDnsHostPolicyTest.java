package com.example.shortener.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetAddress;
import org.junit.jupiter.api.Test;

class MixedDnsHostPolicyTest {

    @Test
    void rejectsWhenAnyResolvedAddressIsPrivate() throws Exception {
        InetAddress publicIp = InetAddress.getByName("8.8.8.8");
        InetAddress privateIp = InetAddress.getByName("10.0.0.5");
        UrlPolicy policy = new UrlPolicy(host -> new InetAddress[] {publicIp, privateIp});

        assertThatThrownBy(() -> policy.validateUrl("https://mixed.example/path"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("URL host is not allowed");
    }

    @Test
    void allowsWhenAllResolvedAddressesArePublic() throws Exception {
        InetAddress a = InetAddress.getByName("1.1.1.1");
        InetAddress b = InetAddress.getByName("8.8.4.4");
        UrlPolicy policy = new UrlPolicy(host -> new InetAddress[] {a, b});

        assertThat(policy.validateUrl("https://multi.example/ok"))
                .isEqualTo("https://multi.example/ok");
    }
}
