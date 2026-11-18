import ipaddress,socket,sys,threading,unittest,uuid
from unittest.mock import patch,MagicMock
from jarvis_companion import DiscoveryAnnouncer,Failure,local_address

class DiscoveryTest(unittest.TestCase):
    def test_public_host_is_rejected(self):
        with self.assertRaises(Failure):DiscoveryAnnouncer().start('8.8.8.8',8765,str(uuid.uuid4()))
    def test_wildcard_is_rejected(self):
        with self.assertRaises(Failure):DiscoveryAnnouncer().start('0.0.0.0',8765,str(uuid.uuid4()))
    def test_loopback_is_not_advertised_to_lan(self):
        with self.assertRaises(Failure):DiscoveryAnnouncer().start('127.0.0.1',8765,str(uuid.uuid4()))
    def test_malformed_identity_is_rejected(self):
        with self.assertRaises(Failure):DiscoveryAnnouncer().start('192.168.1.8',8765,'wrong')
    def test_advertisement_contains_only_public_identity_hints(self):
        with patch('zeroconf.Zeroconf')as factory:
            a=DiscoveryAnnouncer();device=str(uuid.uuid4());self.assertTrue(a.start('192.168.1.8',8765,device))
            self.assertEqual({b'protocol':b'1',b'device':device.encode()},a.info.properties)
            self.assertNotIn(b'token',a.info.properties);self.assertNotIn(b'code',a.info.properties);self.assertNotIn(b'pin',a.info.properties)
            a.close();factory.return_value.unregister_service.assert_called_once()
    def test_registration_failure_preserves_qr_fallback(self):
        with patch('zeroconf.Zeroconf')as factory:
            factory.return_value.register_service.side_effect=OSError('multicast blocked')
            a=DiscoveryAnnouncer();self.assertFalse(a.start('192.168.1.8',8765,str(uuid.uuid4())));self.assertIsNone(a.connection);factory.return_value.close.assert_called_once()
    def test_close_is_idempotent(self):
        a=DiscoveryAnnouncer();a.close();a.close();self.assertIsNone(a.connection)
    @unittest.skipUnless(sys.platform.startswith('linux'),'Actual Linux multicast fixture; no native Windows claim')
    def test_actual_linux_mdns_advertisement_and_resolution(self):
        from zeroconf import Zeroconf,ServiceBrowser,ServiceStateChange,IPVersion
        host=socket.gethostbyname(socket.gethostname())
        if not local_address(host) or host.startswith('127.'):self.skipTest('No private LAN interface available for actual multicast fixture')
        a=DiscoveryAnnouncer();device=str(uuid.uuid4());self.assertTrue(a.start(host,18765,device),a.last_error)
        event=threading.Event();names=[];zc=Zeroconf(interfaces=[host],ip_version=IPVersion.V4Only)
        def handler(zeroconf,service_type,name,state_change):
            if state_change==ServiceStateChange.Added and device in name:names.append(name);event.set()
        browser=ServiceBrowser(zc,a.service_type,handlers=[handler])
        try:
            self.assertTrue(event.wait(6),'Actual mDNS advertisement was not observed on the Linux interface')
            info=zc.get_service_info(a.service_type,names[0],timeout=3000)
            self.assertIsNotNone(info);self.assertEqual(device.encode(),info.properties[b'device']);self.assertEqual(b'1',info.properties[b'protocol']);self.assertEqual(18765,info.port);self.assertIn(host,info.parsed_addresses())
        finally:browser.cancel();zc.close();a.close()
