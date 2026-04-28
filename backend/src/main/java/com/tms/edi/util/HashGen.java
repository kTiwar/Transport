package com.tms.edi.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Temporary utility — run with: mvn -q exec:java -Dexec.mainClass="com.tms.edi.util.HashGen" */
public class HashGen {
    public static void main(String[] args) {
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder(10);
        String password = args.length > 0 ? args[0] : "Admin@2026!";
        System.out.println("BCrypt hash for [" + password + "]:");
        System.out.println(enc.encode(password));
    }
}
