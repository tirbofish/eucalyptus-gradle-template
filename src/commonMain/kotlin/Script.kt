import com.dropbear.DropbearEngine
import com.dropbear.Runnable
import com.dropbear.System

@Runnable(["tag1", "tag2"])
class Script: System() {
    override fun load(engine: DropbearEngine) {
        println("I have awoken")
    }

    override fun update(engine: DropbearEngine, deltaTime: Float) {
        println("Updating!")
    }

    override fun destroy(engine: DropbearEngine) {
        println("I have fallen")
    }
}
