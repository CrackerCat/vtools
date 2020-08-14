package com.omarea.shell_utils

import android.content.Context
import com.omarea.Scene
import com.omarea.common.shared.FileWrite
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.shell.KernelProrp
import com.omarea.common.shell.RootFile
import com.omarea.model.BatteryStatus

/**
 * Created by Hello on 2017/11/01.
 */

class BatteryUtils {
    companion object {
        var isHuawei = false
        var ioInfoSupported = true
        private var fastChargeScript = ""
        private var changeLimitRunning = false
        private var isFirstRun = true
    }

    //获取电池信息
    /*else if (info.startsWith("POWER_SUPPLY_TIME_TO_EMPTY_AVG=")) {
                        stringBuilder.append("平均耗尽 = ");
                        int val = Integer.parseInt(info.substring(keyrowd.length(), info.length()));
                        stringBuilder.append(((val / 3600.0) + "    ").substring(0, 4));
                        stringBuilder.append("小时");
                    } else if (info.startsWith("POWER_SUPPLY_TIME_TO_FULL_AVG=")) {
                        stringBuilder.append("平均充满 = ");
                        int val = Integer.parseInt(info.substring(keyrowd.length(), info.length()));
                        stringBuilder.append(((val / 3600.0) + "    ").substring(0, 4));
                        stringBuilder.append("小时");
                    }*/

    private fun str2voltage(str:String): String {
        return (if (str.length >= 4) {
            str.substring(0, 4).toInt() / 1000f
        } else if (str.length >= 3) {
            str.substring(0, 3).toInt() / 100f
        } else if (str.length >= 2) {
            str.substring(0, 2).toInt() / 10f
        } else {
            return str
        }).toString() + "v"
    }

    val batteryInfo: String
        get() {
            if (RootFile.fileExists("/sys/class/power_supply/bms/uevent")) {
                val batteryInfos = KernelProrp.getProp("/sys/class/power_supply/bms/uevent")
                val infos = batteryInfos.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val stringBuilder = StringBuilder()
                var io = ""
                var mahLength = 0
                for (info in infos) {
                    try {
                        if (info.startsWith("POWER_SUPPLY_CHARGE_FULL=")) {
                            val keyrowd = "POWER_SUPPLY_CHARGE_FULL="
                            stringBuilder.append("充满容量 = ")
                            stringBuilder.append(info.substring(keyrowd.length, keyrowd.length + 4))
                            if (mahLength == 0) {
                                val value = info.substring(keyrowd.length, info.length)
                                mahLength = value.length
                            }
                            stringBuilder.append("mAh")
                        } else if (info.startsWith("POWER_SUPPLY_CHARGE_FULL_DESIGN=")) {
                            val keyrowd = "POWER_SUPPLY_CHARGE_FULL_DESIGN="
                            stringBuilder.append("设计容量 = ")
                            stringBuilder.append(info.substring(keyrowd.length, keyrowd.length + 4))
                            stringBuilder.append("mAh")
                            val value = info.substring(keyrowd.length, info.length)
                            mahLength = value.length
                        } else if (info.startsWith("POWER_SUPPLY_TEMP=")) {
                            val keyrowd = "POWER_SUPPLY_TEMP="
                            stringBuilder.append("电池温度 = ")
                            val temp = info.substring(keyrowd.length, info.length)
                            val prefix = if (temp.contains("-")) "-" else ""
                            val tempStr = temp.replace("-", "")
                            stringBuilder.append(prefix)
                            stringBuilder.append(if (tempStr.length >= 3) {
                                tempStr.substring(0, 3).toInt() / 10f
                            } else {
                                tempStr.substring(0, 2).toInt()
                            })
                            stringBuilder.append("°C")
                        } else if (info.startsWith("POWER_SUPPLY_TEMP_WARM=")) {
                            val keyrowd = "POWER_SUPPLY_TEMP_WARM="
                            stringBuilder.append("警戒温度 = ")
                            val value = Integer.parseInt(info.substring(keyrowd.length, info.length))
                            stringBuilder.append(value / 10)
                            stringBuilder.append("°C")
                        } else if (info.startsWith("POWER_SUPPLY_TEMP_COOL=")) {
                            val keyrowd = "POWER_SUPPLY_TEMP_COOL="
                            stringBuilder.append("低温温度 = ")
                            val value = Integer.parseInt(info.substring(keyrowd.length, info.length))
                            stringBuilder.append(value / 10)
                            stringBuilder.append("°C")
                        } else if (info.startsWith("POWER_SUPPLY_VOLTAGE_NOW=")) {
                            val keyrowd = "POWER_SUPPLY_VOLTAGE_NOW="
                            stringBuilder.append("当前电压 = ")
                            stringBuilder.append(str2voltage(info.substring(keyrowd.length, info.length)))
                        } else if (info.startsWith("POWER_SUPPLY_VOLTAGE_MAX_DESIGN=")) {
                            val keyrowd = "POWER_SUPPLY_VOLTAGE_MAX_DESIGN="
                            stringBuilder.append("设计电压 = ")
                            stringBuilder.append(str2voltage(info.substring(keyrowd.length, info.length)))
                        } else if (info.startsWith("POWER_SUPPLY_VOLTAGE_MIN=")) {
                            val keyrowd = "POWER_SUPPLY_VOLTAGE_MIN="
                            stringBuilder.append("最小电压 = ")
                            stringBuilder.append(str2voltage(info.substring(keyrowd.length, info.length)))
                        } else if (info.startsWith("POWER_SUPPLY_VOLTAGE_MAX=")) {
                            val keyrowd = "POWER_SUPPLY_VOLTAGE_MAX="
                            stringBuilder.append("最大电压 = ")
                            stringBuilder.append(str2voltage(info.substring(keyrowd.length, info.length)))
                        } else if (info.startsWith("POWER_SUPPLY_BATTERY_TYPE=")) {
                            val keyrowd = "POWER_SUPPLY_BATTERY_TYPE="
                            stringBuilder.append("电池类型 = ")
                            stringBuilder.append(info.substring(keyrowd.length, info.length))
                        } else if (info.startsWith("POWER_SUPPLY_CYCLE_COUNT=")) {
                            val keyrowd = "POWER_SUPPLY_CYCLE_COUNT="
                            stringBuilder.append("循环次数 = ")
                            stringBuilder.append(info.substring(keyrowd.length, info.length))
                        } else if (info.startsWith("POWER_SUPPLY_CONSTANT_CHARGE_VOLTAGE=")) {
                            val keyrowd = "POWER_SUPPLY_CONSTANT_CHARGE_VOLTAGE="
                            stringBuilder.append("充电电压 = ")
                            stringBuilder.append(str2voltage(info.substring(keyrowd.length, info.length)))
                        } else if (info.startsWith("POWER_SUPPLY_CAPACITY=")) {
                            val keyrowd = "POWER_SUPPLY_CAPACITY="
                            stringBuilder.append("电池电量 = ")
                            stringBuilder.append(info.substring(keyrowd.length, info.length))
                            stringBuilder.append("%")
                        } else if (info.startsWith("POWER_SUPPLY_MODEL_NAME=")) {
                            val keyrowd = "POWER_SUPPLY_MODEL_NAME="
                            stringBuilder.append("监测模块 = ")
                            stringBuilder.append(info.substring(keyrowd.length, info.length))
                        } else if (info.startsWith("POWER_SUPPLY_RESISTANCE_NOW=")) {
                            val keyrowd = "POWER_SUPPLY_RESISTANCE_NOW="
                            stringBuilder.append("电阻/阻值 = ")
                            stringBuilder.append(info.substring(keyrowd.length, info.length))
                        } /* else if (info.startsWith("POWER_SUPPLY_VOLTAGE_AVG=")) {
                            val keyrowd = "POWER_SUPPLY_VOLTAGE_AVG="
                            stringBuilder.append("平均电压 = ")
                            stringBuilder.append(str2voltage(info.substring(keyrowd.length, info.length)))
                        } */ else if (info.startsWith("POWER_SUPPLY_CURRENT_NOW=")) {
                            val keyrowd = "POWER_SUPPLY_CURRENT_NOW="
                            io = info.substring(keyrowd.length, info.length)
                            continue
                        } else {
                            continue
                        }
                        stringBuilder.append("\n")
                    } catch (ignored: Exception) {
                        stringBuilder.append("\n")
                    }
                }

                if (io.isNotEmpty() && mahLength != 0) {
                    val `val` = if (mahLength < 5) Integer.parseInt(io) else (Integer.parseInt(io) / Math.pow(10.0, (mahLength - 4).toDouble())).toInt()
                    stringBuilder.insert(0, "放电速度 = " + `val` + "mA\n")
                }

                return stringBuilder.toString()
            } else {
                return ""
            }
        }

    val usbInfo: String
        get() {
            if (RootFile.fileExists("/sys/class/power_supply/usb/uevent")) {
                val batteryInfos = KernelProrp.getProp("/sys/class/power_supply/usb/uevent")
                val infos = batteryInfos.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val stringBuilder = StringBuilder()
                var voltage = 0F
                var electricity = 0F
                for (info in infos) {
                    try {
                        if (info.startsWith("POWER_SUPPLY_ONLINE=")) {
                            val keyrowd = "POWER_SUPPLY_ONLINE="
                            stringBuilder.append("USB供电 = ")
                            if (info.substring(keyrowd.length, info.length).equals("1")) {
                                stringBuilder.append("√")
                            } else {
                                stringBuilder.append("×")
                            }
                        } else if (info.startsWith("POWER_SUPPLY_VOLTAGE_NOW=")) {
                            val keyrowd = "POWER_SUPPLY_VOLTAGE_NOW="
                            stringBuilder.append("当前电压 = ")
                            val v = str2voltage(info.substring(keyrowd.length, info.length))
                            voltage = v.replace("v", "").toFloat()
                            stringBuilder.append(v)
                        } /* else if (info.startsWith("POWER_SUPPLY_VOLTAGE_MAX=")) {
                            val keyrowd = "POWER_SUPPLY_VOLTAGE_MAX="
                            stringBuilder.append("最大电压 = ")
                            stringBuilder.append(str2voltage(info.substring(keyrowd.length, info.length)))
                        } else if (info.startsWith("POWER_SUPPLY_VOLTAGE_MAX_DESIGN=")) {
                            val keyrowd = "POWER_SUPPLY_VOLTAGE_MAX_DESIGN="
                            stringBuilder.append("最大电压(设计) = ")
                            stringBuilder.append(str2voltage(info.substring(keyrowd.length, info.length)))
                        } */ else if (info.startsWith("POWER_SUPPLY_CURRENT_MAX=")) {
                            val keyrowd = "POWER_SUPPLY_CURRENT_MAX="
                            val v = Integer.parseInt(info.substring(keyrowd.length, info.length)) / 1000 / 1000.0f
                            if (v > 0) {
                                stringBuilder.append("最大电流 = ")
                                stringBuilder.append(v)
                                stringBuilder.append("A")
                            } else {
                                continue
                            }
                        } else if (info.startsWith("POWER_SUPPLY_PD_VOLTAGE_MAX=")) {
                            val keyrowd = "POWER_SUPPLY_PD_VOLTAGE_MAX="
                            stringBuilder.append("最大电压(PD) = ")
                            stringBuilder.append(str2voltage(info.substring(keyrowd.length, info.length)))
                        } else if (info.startsWith("POWER_SUPPLY_CONNECTOR_TEMP=")) {
                            val keyrowd = "POWER_SUPPLY_CONNECTOR_TEMP="
                            stringBuilder.append("接口温度 = ")
                            val v = Integer.parseInt(info.substring(keyrowd.length, info.length))
                            stringBuilder.append((v / 10.0f))
                            stringBuilder.append("°C")
                        } else if (info.startsWith("POWER_SUPPLY_PD_VOLTAGE_MIN=")) {
                            val keyrowd = "POWER_SUPPLY_PD_VOLTAGE_MIN="
                            stringBuilder.append("最小电压(PD) = ")
                            stringBuilder.append(str2voltage(info.substring(keyrowd.length, info.length)))
                        } else if (info.startsWith("POWER_SUPPLY_PD_CURRENT_MAX=")) {
                            val keyrowd = "POWER_SUPPLY_PD_CURRENT_MAX="
                            val v = Integer.parseInt(info.substring(keyrowd.length, info.length)) / 1000 / 1000.0f
                            if (v > 0) {
                                stringBuilder.append("最大电流(PD) = ")
                                stringBuilder.append(v)
                                stringBuilder.append("A")
                            } else {
                                continue
                            }
                        } else if (info.startsWith("POWER_SUPPLY_INPUT_CURRENT_NOW=")) {
                            val keyrowd = "POWER_SUPPLY_INPUT_CURRENT_NOW="
                            stringBuilder.append("当前电流 = ")
                            val v = Integer.parseInt(info.substring(keyrowd.length, info.length))
                            electricity = v / 1000 / 1000.0f
                            stringBuilder.append(electricity)
                            stringBuilder.append("A")
                        } else if (info.startsWith("POWER_SUPPLY_QUICK_CHARGE_TYPE=")) {
                            val keyrowd = "POWER_SUPPLY_QUICK_CHARGE_TYPE="
                            stringBuilder.append("快充类型 = ")
                            val type = info.substring(keyrowd.length, info.length)
                            if (type == "0") {
                                stringBuilder.append("慢速充电")
                            } else {
                                stringBuilder.append("类型")
                                stringBuilder.append(type)
                            }
                        } else if (info.startsWith("POWER_SUPPLY_REAL_TYPE=")) {
                            val keyrowd = "POWER_SUPPLY_REAL_TYPE="
                            stringBuilder.append("实际类型 = ")
                            stringBuilder.append(info.substring(keyrowd.length, info.length))
                        } else if (info.startsWith("POWER_SUPPLY_HVDCP3_TYPE=")) {
                            val keyrowd = "POWER_SUPPLY_HVDCP3_TYPE="
                            stringBuilder.append("高压快充 = ")
                            val type = info.substring(keyrowd.length, info.length)
                            if (type == "0") {
                                stringBuilder.append("否")
                            } else {
                                stringBuilder.append("类型")
                                stringBuilder.append(type)
                            }
                        } else if (info.startsWith("POWER_SUPPLY_PD_AUTHENTICATION=")) {
                            val keyrowd = "POWER_SUPPLY_PD_AUTHENTICATION="
                            stringBuilder.append("PD认证 = ")
                            stringBuilder.append(if (info.substring(keyrowd.length, info.length).equals("1")) "已认证" else "未认证")
                        } else {
                            continue
                        }
                        stringBuilder.append("\n")
                    } catch (ignored: Exception) {
                        stringBuilder.append("\n")
                    }
                }
                if (voltage > 0 && electricity > 0) {
                    stringBuilder.append("\n参考功率 = ")
                    stringBuilder.append((voltage * electricity * 100).toInt() / 100f)
                    stringBuilder.append("W")
                }

                return stringBuilder.toString()
            } else {
                return ""
            }
        }

    //快充是否支持修改充电速度设置
    fun qcSettingSupport(): Boolean {
        return RootFile.itemExists("/sys/class/power_supply/battery/constant_charge_current_max")
    }

    fun stepChargeSupport(): Boolean {
        return RootFile.itemExists("/sys/class/power_supply/battery/step_charging_enabled")
    }

    fun getStepCharge(): Boolean {
        return KernelProrp.getProp("/sys/class/power_supply/battery/step_charging_enabled").equals("1")
    }

    fun setStepCharge(stepCharge: Boolean) {
        KernelProrp.setProp("/sys/class/power_supply/battery/step_charging_enabled", if (stepCharge) "1" else "0")
    }

    private var useMainConstant:Boolean? = false // null
    fun getqcLimit(): String {
        if (useMainConstant == null) {
            val value = RootFile.fileExists("/sys/class/power_supply/main/constant_charge_current_max")
            useMainConstant = RootFile.fileExists("/sys/class/power_supply/main/constant_charge_current_max")
        }

        var limit = if (useMainConstant == true) {
            KernelProrp.getProp("/sys/class/power_supply/main/constant_charge_current_max")
        } else {
            KernelProrp.getProp("/sys/class/power_supply/battery/constant_charge_current_max")
        }
        if (limit.length > 3) {
            limit = limit.substring(0, limit.length - 3) + "mA"
        } else if (limit.isNotEmpty()) {
            try {
                if (Integer.parseInt(limit) == 0) {
                    limit = "0"
                }
            } catch (ignored: Exception) {
            }

        } else {
            return "?mA"
        }
        return limit
    }

    //快充是否支持电池保护
    fun bpSettingSupport(): Boolean {
        return RootFile.itemExists("/sys/class/power_supply/battery/battery_charging_enabled") || RootFile.itemExists("/sys/class/power_supply/battery/input_suspend")
    }

    // 设置充电速度限制
    fun setChargeInputLimit(limit: Int, context: Context): Boolean {
        if (changeLimitRunning) {
            return false
        } else {
            synchronized(Scene.context) {
                changeLimitRunning = true

                if (fastChargeScript.isEmpty()) {
                    val output = FileWrite.writePrivateShellFile("addin/fast_charge.sh", "addin/fast_charge.sh", context)
                    val output2 = FileWrite.writePrivateShellFile("addin/fast_charge_run_once.sh", "addin/fast_charge_run_once.sh", context)
                    if (output != null && output2 != null) {
                        if (isFirstRun) {
                            KeepShellPublic.getInstance("setChargeInputLimit", true).doCmdSync("sh " + output2)
                            isFirstRun = false
                        }

                        fastChargeScript = "sh $output "
                    }
                }

                return if (fastChargeScript.isNotEmpty()) {
                    if (limit > 3000) {
                        var current = 3000
                        while (current < (limit - 300) && current < 5000) {
                            KeepShellPublic.getInstance("setChargeInputLimit", true).doCmdSync(fastChargeScript + current + " 1")
                            current += 300
                        }
                    }
                    KeepShellPublic.getInstance("setChargeInputLimit", true).doCmdSync(fastChargeScript + limit + " 0")
                    changeLimitRunning = false
                    true
                } else {
                    changeLimitRunning = false
                    false
                }
            }
        }
    }

    fun pdSupported(): Boolean {
        return RootFile.fileExists("/sys/class/power_supply/usb/pd_allowed") || RootFile.fileExists("/sys/class/power_supply/usb/pd_active")
    }

    fun pdAllowed(): Boolean {
        return KernelProrp.getProp("/sys/class/power_supply/usb/pd_allowed") == "1"
    }

    fun setAllowed(boolean: Boolean): Boolean {
        val builder = java.lang.StringBuilder()
        builder.append("chmod 777 /sys/class/power_supply/usb/pd_allowed\n")
        builder.append("echo ${if (boolean) "1" else "0"}> /sys/class/power_supply/usb/pd_allowed\n")
        builder.append("chmod 777 /sys/class/power_supply/usb/pd_active\n")
        builder.append("echo 1 > /sys/class/power_supply/usb/pd_active\n")
        return KeepShellPublic.doCmdSync(builder.toString()) != "error"
    }

    fun pdActive(): Boolean {
        return KernelProrp.getProp("/sys/class/power_supply/usb/pd_active") == "1"
    }

    /**
     * 获取电池温度
     */
    public fun getBatteryTemperature(): BatteryStatus {
        val batteryInfo = KeepShellPublic.doCmdSync("dumpsys battery")
        val batteryInfos = batteryInfo.split("\n")

        // 由于部分手机相同名称的参数重复出现，并且值不同，为了避免这种情况，加个额外处理，同名参数只读一次
        var levelReaded = false
        var tempReaded = false
        var statusReaded = false
        val batteryStatus = BatteryStatus()

        for (item in batteryInfos) {
            val info = item.trim()
            val index = info.indexOf(":")
            if (index > Int.MIN_VALUE && index < info.length - 1) {
                val value = info.substring(info.indexOf(":") + 1).trim()
                try {
                    if (info.startsWith("status")) {
                        if (!statusReaded) {
                            batteryStatus.statusText = value
                            statusReaded = true
                        } else {
                            continue
                        }
                    } else if (info.startsWith("level")) {
                        if (!levelReaded) {
                            batteryStatus.level = value.toInt()
                            levelReaded = true
                        } else continue
                    } else if (info.startsWith("temperature")) {
                        if (!tempReaded) {
                            tempReaded = true
                            batteryStatus.temperature = (value.toFloat() / 10.0).toFloat()
                        } else continue
                    }
                } catch (ex: java.lang.Exception) {

                }
            }
        }
        return batteryStatus
    }

    var batteryIOFilePath: String? = null
    private fun getBatteryIOForHuawei(): String {
        if (RootFile.itemExists("/sys/class/power_supply/Battery/uevent")) {
            batteryIOFilePath = "/sys/class/power_supply/Battery/uevent"
        } else if (RootFile.itemExists("/sys/class/power_supply/bms/uevent")) {
            batteryIOFilePath = "/sys/class/power_supply/bms/uevent"
        } else {
            return ""
        }

        try {
            val io = KernelProrp.getProp(batteryIOFilePath!!, "POWER_SUPPLY_CURRENT_NOW=")
            return io.replace("POWER_SUPPLY_CURRENT_NOW=", "").replace("error", "")
        } catch (ex: Exception) {
            return ""
        }
    }

    private fun getBatterySensor(): String? {
        var batterySensor: String? = null
        if (batterySensor == "init") {
            batterySensor = KeepShellPublic.doCmdSync("for sensor in /sys/class/thermal/*; do\n" +
                    "\ttype=\"\$(cat \$sensor/type)\"\n" +
                    "\tif [[ \"\$type\" = \"battery\" && -f \"\$sensor/temp\" ]]; then\n" +
                    "\t\techo \"\$sensor/temp\";\n" +
                    "\t\texit 0;\n" +
                    "\tfi;\n" +
                    "done;")
            if (batterySensor != "error") {
                batterySensor = batterySensor.trim()
            } else {
                batterySensor = null
            }
        }
        return batterySensor
    }

    private var batteryUnit = Int.MIN_VALUE
    private fun getBatteryUnit(): Int {
        if (batteryUnit == Int.MIN_VALUE) {
            val full = KernelProrp.getProp("/sys/class/power_supply/battery/charge_full_design")
            if (full.length >= 4) {
                return full.length - 4
            }
            batteryUnit = Int.MIN_VALUE
        }
        return batteryUnit
    }

    public fun getBatteryIOMa(): String {
        val ioStr = getBatteryIOOrigin()
        if (ioStr == null) {
            return "?"
        } else {
            try {
                var io = ioStr
                val unit = getBatteryUnit()
                var start = ""
                if (io.startsWith("+")) {
                    start = "+"
                    io = io.substring(1, io.length)
                } else if (io.startsWith("-")) {
                    start = "-"
                    io = io.substring(1, io.length)
                } else {
                    start = ""
                }
                if (unit != Int.MIN_VALUE && io.length > unit) {
                    val v = io.substring(0, io.length - unit)
                    if (v.length > 4) {
                        return (start + v.substring(0, v.length - 3))
                    }
                    return (start + v)
                } else if (io.length <= 4) {
                    return (start + io)
                } else if (io.length >= 8) {
                    return (start + io.substring(0, io.length - 6))
                } else if (io.length >= 5) {
                    return (start + io.substring(0, io.length - 3))
                }
                return (start + io)
            } catch (ex: Exception) {
                return "?"
            }
        }
    }

    public fun getBatteryIOOrigin(): String? {
        if (isHuawei) {
            return getBatteryIOForHuawei()
        }
        if (ioInfoSupported && batteryIOFilePath == null) {
            if (RootFile.itemExists("/sys/class/power_supply/battery/current_now")) {
                batteryIOFilePath = "/sys/class/power_supply/battery/current_now"
            } else if (RootFile.itemExists("/sys/class/power_supply/battery/BatteryAverageCurrent")) {
                batteryIOFilePath = "/sys/class/power_supply/battery/BatteryAverageCurrent"
            } else {
                val io = getBatteryIOForHuawei()
                if (!io.isEmpty()) {
                    isHuawei = true
                } else {
                    ioInfoSupported = false
                }
                return io;
            }
        }
        if (!ioInfoSupported) {
            return null
        }
        val io = KernelProrp.getProp(batteryIOFilePath!!)
        if (io.isEmpty()) {
            return null
        } else {
            return io;
        }
    }

    public fun getChargeFull(): Int {
        val value = KernelProrp.getProp("/sys/class/power_supply/bms/charge_full")
        return if (Regex("^[0-9]+").matches(value)) (value.toInt() / 1000) else 0
    }

    public fun setChargeFull(mAh: Int) {
        KernelProrp.setProp("/sys/class/power_supply/bms/charge_full", (mAh * 1000).toString())
    }

    public fun getCpacity(): Int {
        val value = KernelProrp.getProp("/sys/class/power_supply/battery/capacity")
        return if (Regex("^[0-9]+").matches(value)) value.toInt() else 0
    }

    public fun setCapacity(capacity: Int) {
        KernelProrp.setProp("/sys/class/power_supply/battery/capacity", capacity.toString())
    }

    private var kernelCapacitySupported: Boolean? = null
    // 从内核读取可以精确到0.01的电量，但有些内核数值是错的，所以需要和系统反馈的电量(approximate)比对，如果差距太大则认为内核数值无效，不再读取
    public fun getKernelCapacity(approximate:Int): Float {
        if (kernelCapacitySupported == null) {
            kernelCapacitySupported = RootFile.fileExists("/sys/class/power_supply/bms/capacity_raw")
        }
        if (kernelCapacitySupported == true) {
            try {
                val capacity_raw = KernelProrp.getProp("/sys/class/power_supply/bms/capacity_raw")
                val capacityValue = (capacity_raw).toInt()

                val valueMA = if (Math.abs(capacityValue - approximate) > Math.abs((capacityValue / 100f) - approximate)) {
                    capacityValue / 100f
                } else {
                    capacity_raw.toFloat()
                }
                // 如果和系统反馈的电量差距超过5%，则认为数值无效，不再读取
                if (Math.abs(valueMA - approximate) > 5) {
                    kernelCapacitySupported = false;
                    return  -1f
                } else {
                    return valueMA
                }
            } catch (ex: java.lang.Exception) {
                kernelCapacitySupported = false;
            }
        }
        return -1f
    }
}
