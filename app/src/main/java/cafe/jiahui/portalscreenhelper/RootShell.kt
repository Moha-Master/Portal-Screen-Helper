package cafe.jiahui.portalscreenhelper

import java.io.DataOutputStream
import java.io.IOException

object RootShell {
    private var process: Process? = null

    fun execute(command: String): String? {
        return try {
            if (process == null || !process!!.isAlive) {
                process = Runtime.getRuntime().exec("su")
            }

            val os = DataOutputStream(process!!.outputStream)
            val inputStream = process!!.inputStream
            val errorStream = process!!.errorStream

            os.writeBytes(command + "\n")
            os.flush()

            // Add a marker to know when the command output ends
            val endMarker = "END_OF_COMMAND_OUTPUT"
            os.writeBytes("echo $endMarker\n")
            os.flush()

            val output = StringBuilder()
            val reader = inputStream.bufferedReader()

            while (true) {
                val line = reader.readLine() ?: break
                if (line == endMarker) {
                    break
                }
                output.append(line).append("\n")
            }
            
            // Also check for errors
            if (errorStream.available() > 0) {
                val errorReader = errorStream.bufferedReader()
                while (true) {
                    val line = errorReader.readLine() ?: break
                    // For now, just print errors to logcat, or handle them as needed
                    android.util.Log.e("RootShell", "Error: $line")
                }
            }

            output.toString().trim()
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun close() {
        try {
            process?.destroy()
            process = null
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
