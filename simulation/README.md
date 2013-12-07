Task 2 : Network Simulation
=========

* report: report files
* OurSuomi: OMNeT++ simulation project

Hacks to OMNeT++
---------
* OMNeT++ OSPFv2 does not support 'passive' interfaces.
* OMNeT++ IPv4 serializer does not support OSPF messages.
* PcapRecorder/PcapDumer uses IPv4 serializer.
* IPv4 serialization of OSPF (0x58/89) messages
 src/util/headerserializers/ipv4/IPv4Serializer.cc:
  Add a case to the big switch-case in IPv4Serializer::serialize():

    case IP_PROT_OSPF:
        break;
