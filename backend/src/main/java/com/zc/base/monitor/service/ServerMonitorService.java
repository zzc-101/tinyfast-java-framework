package com.zc.base.monitor.service;

import lombok.Data;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 服务器监控服务
 */
@Service
public class ServerMonitorService {

    /**
     * 获取服务器信息
     */
    public ServerInfo getServerInfo() {
        ServerInfo info = new ServerInfo();

        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        OperatingSystem os = si.getOperatingSystem();

        // CPU 信息
        info.setCpu(getCpuInfo(hal.getProcessor()));

        // 内存信息
        info.setMemory(getMemoryInfo(hal.getMemory()));

        // JVM 信息
        info.setJvm(getJvmInfo());

        // 系统信息
        info.setSystem(getSystemInfo(os));

        // 磁盘信息
        info.setDisks(getDiskInfo(os));

        return info;
    }

    private CpuInfo getCpuInfo(CentralProcessor processor) {
        CpuInfo cpu = new CpuInfo();
        cpu.setCoreCount(processor.getLogicalProcessorCount());
        cpu.setModelName(processor.getProcessorIdentifier().getName());

        long[] prevTicks = processor.getSystemCpuLoadTicks();
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        double cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100;
        cpu.setUsageRate(Math.round(cpuLoad * 100.0) / 100.0);

        return cpu;
    }

    private MemoryInfo getMemoryInfo(GlobalMemory memory) {
        MemoryInfo mem = new MemoryInfo();
        long totalByte = memory.getTotal();
        long availableByte = memory.getAvailable();
        mem.setTotal(formatBytes(totalByte));
        mem.setUsed(formatBytes(totalByte - availableByte));
        mem.setFree(formatBytes(availableByte));
        mem.setUsageRate(Math.round((totalByte - availableByte) * 10000.0 / totalByte) / 100.0);
        return mem;
    }

    private JvmInfo getJvmInfo() {
        JvmInfo jvm = new JvmInfo();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

        long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();

        jvm.setHeapUsed(formatBytes(heapUsed));
        jvm.setHeapMax(formatBytes(heapMax));
        jvm.setHeapUsageRate(Math.round(heapUsed * 10000.0 / heapMax) / 100.0);
        jvm.setNonHeapUsed(formatBytes(memoryMXBean.getNonHeapMemoryUsage().getUsed()));

        jvm.setJavaVersion(System.getProperty("java.version"));
        jvm.setJvmVendor(System.getProperty("java.vm.vendor"));

        Instant startTime = Instant.ofEpochMilli(runtimeMXBean.getStartTime());
        Duration uptime = Duration.between(startTime, Instant.now());
        jvm.setUptime(formatDuration(uptime));

        jvm.setThreadCount(ManagementFactory.getThreadMXBean().getThreadCount());

        return jvm;
    }

    private SystemInfoDetail getSystemInfo(OperatingSystem os) {
        SystemInfoDetail sys = new SystemInfoDetail();
        Properties props = System.getProperties();
        sys.setOsName(os.toString());
        sys.setOsArch(props.getProperty("os.arch"));
        try {
            InetAddress addr = InetAddress.getLocalHost();
            sys.setHostName(addr.getHostName());
            sys.setHostIp(addr.getHostAddress());
        } catch (Exception e) {
            sys.setHostName("unknown");
            sys.setHostIp("unknown");
        }
        sys.setUserDir(props.getProperty("user.dir"));
        return sys;
    }

    private List<DiskInfo> getDiskInfo(OperatingSystem os) {
        List<DiskInfo> disks = new ArrayList<>();
        FileSystem fileSystem = os.getFileSystem();
        for (OSFileStore store : fileSystem.getFileStores()) {
            DiskInfo disk = new DiskInfo();
            disk.setMount(store.getMount());
            disk.setType(store.getType());
            disk.setTotal(formatBytes(store.getTotalSpace()));
            long usable = store.getUsableSpace();
            long used = store.getTotalSpace() - usable;
            disk.setUsed(formatBytes(used));
            disk.setFree(formatBytes(usable));
            if (store.getTotalSpace() > 0) {
                disk.setUsageRate(Math.round(used * 10000.0 / store.getTotalSpace()) / 100.0);
            }
            disks.add(disk);
        }
        return disks;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        return String.format("%d天%d小时%d分钟", days, hours, minutes);
    }

    // ========== 内部数据类 ==========

    @Data
    public static class ServerInfo {
        private CpuInfo cpu;
        private MemoryInfo memory;
        private JvmInfo jvm;
        private SystemInfoDetail system;
        private List<DiskInfo> disks;
    }

    @Data
    public static class CpuInfo {
        private int coreCount;
        private String modelName;
        private double usageRate;
    }

    @Data
    public static class MemoryInfo {
        private String total;
        private String used;
        private String free;
        private double usageRate;
    }

    @Data
    public static class JvmInfo {
        private String heapUsed;
        private String heapMax;
        private double heapUsageRate;
        private String nonHeapUsed;
        private String javaVersion;
        private String jvmVendor;
        private String uptime;
        private int threadCount;
    }

    @Data
    public static class SystemInfoDetail {
        private String osName;
        private String osArch;
        private String hostName;
        private String hostIp;
        private String userDir;
    }

    @Data
    public static class DiskInfo {
        private String mount;
        private String type;
        private String total;
        private String used;
        private String free;
        private double usageRate;
    }

}
