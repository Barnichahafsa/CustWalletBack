package org.bits.diamabankwalletf.filter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.util.*;

public class SensitiveDataRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] body;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public SensitiveDataRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);

        // Read the original body
        InputStream inputStream = request.getInputStream();
        body = inputStream.readAllBytes();
    }

    @Override
    public ServletInputStream getInputStream() {
        // Create a new input stream from our stored body
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body);

        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return byteArrayInputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // Not used in this implementation
            }

            @Override
            public int read() {
                return byteArrayInputStream.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    // Method to access the body for logging (with sensitive data masked)
    public String getMaskedBody() {
        try {
            String bodyStr = new String(body);

            // Simple string replacement for logging
            return bodyStr.replaceAll("\"password\":\"[^\"]*\"", "\"password\":\"[MASKED]\"");
        } catch (Exception e) {
            return "[Error reading body]";
        }
    }
}
