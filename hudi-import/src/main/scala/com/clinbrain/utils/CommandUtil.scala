package com.clinbrain.utils

import java.io.{FileOutputStream, OutputStream}

import org.apache.commons.exec.{CommandLine, DefaultExecutor, ExecuteWatchdog, PumpStreamHandler}

/**
 * @ClassName CommandUtil
 * @Description TODO
 * @Author p
 * @Date 2020/5/25 18:29
 * @Version 1.0
 **/
object CommandUtil {

    /**
     * 执行指定命令
     *
     * @param command 命令
     * @return 命令执行完成返回结果
     */
    def execCommand(command: String, logFilePath: String): Int = {
        val out = new FileOutputStream(logFilePath, true)
        val exitCode = execCommand(command, out)
        out.close()
        exitCode
    }

    /**
     * 执行指定命令，输出结果到指定输出流中
     *
     * @param command 命令
     * @param out     执行结果输出流
     * @return 执行结果状态码：执行成功返回0
     */

    def execCommand(command: String, out: OutputStream): Int = {
        val commandLine = CommandLine.parse(command)
        val pumpStreamHandler = if (null == out) {
            new PumpStreamHandler
        } else {
            new PumpStreamHandler(out)
        }

        // 设置超时时间为10秒
        val watchdog = new ExecuteWatchdog(10000)
        val executor = new DefaultExecutor
        executor.setStreamHandler(pumpStreamHandler)
        executor.setWatchdog(watchdog)
        executor.execute(commandLine)
    }
}
