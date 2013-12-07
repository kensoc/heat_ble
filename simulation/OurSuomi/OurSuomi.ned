import inet.util.NetAnimTrace;
import inet.nodes.inet.StandardHost;
import inet.networklayer.autorouting.ipv4.IPv4NetworkConfigurator;
import inet.base.LifecycleController;
import inet.nodes.ethernet.EtherSwitch;
import inet.nodes.ospfv2.OSPFRouter;
import inet.util.ThruputMeteringChannel;
import inet.world.scenario.ScenarioManager;


network OurSuomi
{
    parameters:
        @display("p=10,10;b=712,152");
    types:
        channel CoreFibre extends ThruputMeteringChannel
        {
            delay = 1ms;
            datarate = 20Mbps;
            thruputDisplayFormat = "#N";
            @display("ls=red,2");
        }
        channel LANTx extends ThruputMeteringChannel
        {
            datarate = 100Mbps;
            thruputDisplayFormat = "#N";
            @display("ls=purple,1");
        }
        channel CDNTx extends ThruputMeteringChannel
        {
            datarate = 1Gbps;
            thruputDisplayFormat = "#N";
            @display("ls=green,3");
        }
    submodules:
        rHelsinki: OSPFRouter {
            @display("p=524,382");
            gates:
                ethg[1];
                pppg[3];
        }
        rTurku: OSPFRouter {
            @display("p=304,382");
            gates:
                ethg[2];
                pppg[3];
        }
        rTampere: OSPFRouter {
            @display("p=410,250");
            gates:
                pppg[4];
        }
        rLappeenranta: OSPFRouter {
            @display("p=588,256");
            gates:
                ethg[2];
                pppg[3];
        }
        rVaasa: OSPFRouter {
            @display("p=304,134");
            gates:
                ethg[2];
                pppg[2];
        }
        rOulu: OSPFRouter {
            @display("p=472,103");
            gates:
                ethg[1];
                pppg[2];
        }
        rGateway: OSPFRouter {
            @display("p=222,413");
            gates:
                pppg[1];
        }
        sLANHelsinki: EtherSwitch {
            @display("p=573,431");
            gates:
                ethg[1];
        }
        sLANTurku: EtherSwitch {
            @display("p=233,333");
            gates:
                ethg[2];
        }
        sCDNTurku: EtherSwitch {
            @display("p=334,431");
            gates:
                ethg[1];
        }
        sLANLappeenranta: EtherSwitch {
            @display("p=644,307");
            gates:
                ethg[1];
        }
        sCDNLappeenranta: EtherSwitch {
            @display("p=644,227");
            gates:
                ethg[1];
        }
        sLANVaasa: EtherSwitch {
            @display("p=239,98");
            gates:
                ethg[1];
        }
        sCDNVaasa: EtherSwitch {
            @display("p=239,179");
            gates:
                ethg[1];
        }
        sLANOulu: EtherSwitch {
            @display("p=443,42");
            gates:
                ethg[2];
        }
        tcpSource: StandardHost {
            @display("p=162,334");
            gates:
                ethg[1];
        }
        tcpSink: StandardHost {
            @display("p=563,43");
            gates:
                ethg[1];
        }
        configurator: IPv4NetworkConfigurator {
            parameters:
                @display("p=75,43");
        }
    connections:
        rHelsinki.pppg[0] <--> CoreFibre <--> rTurku.pppg[0];
        rHelsinki.pppg[2] <--> CoreFibre <--> rLappeenranta.pppg[2];
        rHelsinki.pppg[1] <--> CoreFibre <--> rTampere.pppg[1];
        rTurku.pppg[1] <--> CoreFibre <--> rTampere.pppg[0];
        rTurku.pppg[2] <--> CoreFibre <--> rGateway.pppg[0];
        rTampere.pppg[2] <--> CoreFibre <--> rLappeenranta.pppg[0];
        rTampere.pppg[3] <--> CoreFibre <--> rVaasa.pppg[1];
        rLappeenranta.pppg[1] <--> CoreFibre <--> rOulu.pppg[1];
        rVaasa.pppg[0] <--> CoreFibre <--> rOulu.pppg[0];
        sLANHelsinki.ethg[0] <--> LANTx <--> rHelsinki.ethg[0];
        sLANTurku.ethg[0] <--> LANTx <--> rTurku.ethg[1];
        sCDNTurku.ethg[0] <--> CDNTx <--> rTurku.ethg[0];
        sLANLappeenranta.ethg[0] <--> LANTx <--> rLappeenranta.ethg[0];
        sCDNLappeenranta.ethg[0] <--> CDNTx <--> rLappeenranta.ethg[1];
        sLANVaasa.ethg[0] <--> LANTx <--> rVaasa.ethg[0];
        sCDNVaasa.ethg[0] <--> CDNTx <--> rVaasa.ethg[1];
        sLANOulu.ethg[0] <--> LANTx <--> rOulu.ethg[0];
        tcpSource.ethg[0] <--> LANTx <--> sLANTurku.ethg[1];
        tcpSink.ethg[0] <--> LANTx <--> sLANOulu.ethg[1];
}
