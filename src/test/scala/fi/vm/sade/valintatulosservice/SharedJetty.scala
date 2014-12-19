package fi.vm.sade.valintatulosservice
import fi.vm.sade.utils.tcp.PortChecker

object SharedJetty {
   private lazy val jettyLauncher = new JettyLauncher(PortChecker.findFreeLocalPort, Some("it"))

   def port = jettyLauncher.port

   def start {
     jettyLauncher.start
   }
 }
