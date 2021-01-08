package com.mutao.pay;

import com.mutao.pay.sdk.SDKConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PayApplication {

	public static void main(String[] args) {
		SDKConfig.getConfig().loadPropertiesFromPath(null);
		SpringApplication.run(PayApplication.class, args);
	}

}
