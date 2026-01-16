package net.mbi.wcloud.dispatch.solver;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
@MapperScan("net.mbi.wcloud.dispatch.solver.dal.mysql")
public class DispatchSolverApplication {

	public static void main(String[] args) {
		SpringApplication.run(DispatchSolverApplication.class, args);
	}
}