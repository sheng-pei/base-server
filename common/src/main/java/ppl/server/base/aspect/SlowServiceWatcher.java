package ppl.server.base.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import ppl.common.utils.watch.StopWatch;

import java.util.concurrent.TimeUnit;

//TODO, 增强计时功能，堆栈计时支持
@Aspect
@Order
public class SlowServiceWatcher {
    private static final Logger logger = LoggerFactory.getLogger(SlowServiceWatcher.class);
    private static final int SLOW_SERVICE_THRESHOLD = 10;

    @Pointcut("(" +
            "@annotation(org.springframework.web.bind.annotation.GetMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.RequestMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping) ||" +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) ||" +
            "@annotation(org.springframework.web.bind.annotation.PatchMapping)" +
            ")")
    private void pointCut() {
    }

    @Around("pointCut()")
    public Object watch(ProceedingJoinPoint pjp) throws Throwable {
        StopWatch watch = StopWatch.createStopWatch();
        try {
            return pjp.proceed();
        } finally {
            long seconds = watch.elapse(TimeUnit.SECONDS);
            if (seconds >= SLOW_SERVICE_THRESHOLD) {
                logger.warn("This api method execute time is {}s, too long. " +
                        "Please optimize it and make execute time under {}s", seconds, SLOW_SERVICE_THRESHOLD);
            }
        }
    }
}
