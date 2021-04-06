
import scala.collection.mutable.HashMap

object PortAvailabilityMapper {

  private val PortMap: HashMap[Int, Boolean] = HashMap(4000 -> true, 4001 -> true, 4002 -> true, 4003 -> true)

  def getAvailablePort: Int ={
    val portsAvailable = PortMap.filter(item => item._2)
    // if all ports are occupied return -1
    if(portsAvailable.isEmpty) return -1
    // if some port are available return the first port in the list
    val portSetup = portsAvailable.head
    val port = portSetup._1
    PortMap.update(port, false)
    port
  }

  def freePort(port: Int): Unit ={
    PortMap.update(port, true)
  }

}
