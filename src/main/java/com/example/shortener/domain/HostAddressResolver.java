package com.example.shortener.domain;

import java.net.InetAddress;
import java.net.UnknownHostException;

@FunctionalInterface
public interface HostAddressResolver {
    InetAddress[] resolveAll(String host) throws UnknownHostException;
}
