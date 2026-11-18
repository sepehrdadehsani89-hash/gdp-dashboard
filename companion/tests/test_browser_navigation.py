"""Replay real-world CDP event orderings without a browser process."""
import json
import unittest
from collections import deque
from jarvis_companion import Failure, verify_browser_navigation

URL = 'https://www.google.com/search?q=local+fixture'
FRAME = {'frameId': 'page', 'loaderId': 'requested'}


def loaded(**change):
    return {'method': 'Page.lifecycleEvent', 'params': {**FRAME, 'name': 'load', **change}}


def response(**change):
    return {'method': 'Network.responseReceived',
            'params': {**FRAME, 'type': 'Document', 'response': {'status': 200, 'url': URL}, **change}}


def navigation(**change):
    return {'id': 4, 'result': {**FRAME, **change}}


def run_events(events, *, observed=None, url=URL, subscriptions=None, action='desktop.search_web'):
    pending = deque()
    requested = []
    def call(method, arguments):
        requested.append(method)
        seq = len(requested)
        if seq <= 3:
            pending.append((subscriptions or {}).get(seq, {'id': seq, 'result': {}}))
        elif seq == 4:
            pending.extend(events)
        else:
            value = {'url': url, 'document': url, 'ready': 'complete'} if observed is None else observed
            pending.append({'id': seq, 'result': {'result': {'value': json.dumps(value)}}})
        return seq

    def receive():
        if not pending:
            raise Failure('PAGE_LOAD_TIMEOUT')
        message = pending.popleft()
        if isinstance(message, Exception):
            raise message
        return message

    return verify_browser_navigation(action, url, call, receive)


class BrowserNavigationTest(unittest.TestCase):
    def test_fast_document_finishes_before_navigation_ack(self):
        self.assertEqual(200, run_events([response(), loaded(), navigation()])['http_status'])

    def test_network_response_arrives_after_load_event(self):
        self.assertEqual(200, run_events([navigation(), loaded(), response()])['http_status'])

    def test_network_and_page_events_can_precede_navigation_ack(self):
        self.assertEqual(200, run_events([loaded(), response(), navigation()])['http_status'])

    def test_about_blank_needs_no_http_status(self):
        result = run_events([navigation(), loaded()], url='about:blank', action='desktop.open_app')
        self.assertIsNone(result['http_status'])

    def assert_failure(self, expected, events, **kwargs):
        with self.assertRaises(Failure) as failure:
            run_events(events, **kwargs)
        self.assertEqual(expected, failure.exception.code)

    def test_page_enable_rejected(self):
        self.assert_failure('BROWSER_UNVERIFIED', [], subscriptions={1: {'id': 1, 'error': {}}})

    def test_network_enable_rejected(self):
        self.assert_failure('BROWSER_UNVERIFIED', [], subscriptions={2: {'id': 2, 'error': {}}})

    def test_lifecycle_enable_rejected(self):
        self.assert_failure('BROWSER_UNVERIFIED', [], subscriptions={3: {'id': 3}})

    def test_other_frame_load_cannot_complete(self):
        self.assert_failure('PAGE_LOAD_TIMEOUT', [navigation(), response(), loaded(frameId='child')])

    def test_previous_loader_cannot_complete(self):
        self.assert_failure('PAGE_LOAD_TIMEOUT', [navigation(), response(), loaded(loaderId='previous')])

    def test_domcontentloaded_is_insufficient(self):
        self.assert_failure('PAGE_LOAD_TIMEOUT', [navigation(), response(), loaded(name='DOMContentLoaded')])

    def test_subresource_http_200_is_insufficient(self):
        self.assert_failure('PAGE_LOAD_TIMEOUT', [navigation(), response(type='Image'), loaded()])

    def test_old_document_status_is_insufficient(self):
        self.assert_failure('PAGE_LOAD_TIMEOUT', [navigation(), response(loaderId='previous'), loaded()])

    def test_navigation_cancelled(self):
        self.assert_failure('CANCELLED', [navigation(), Failure('CANCELLED')])

    def test_navigation_download_is_not_a_page(self):
        self.assert_failure('PAGE_LOAD_FAILED', [navigation(isDownload=True)])

    def test_browser_rejects_url(self):
        self.assert_failure('PAGE_LOAD_FAILED', [navigation(errorText='net::ERR_ABORTED')])

    def test_missing_frame_identity(self):
        self.assert_failure('PAGE_LOAD_FAILED', [navigation(frameId='')])

    def test_unbound_loader_identity(self):
        self.assert_failure('PAGE_LOAD_FAILED', [navigation(loaderId='')])

    def test_document_404(self):
        self.assert_failure('PAGE_HTTP_ERROR', [navigation(), response(response={'url': URL, 'status': 404}), loaded()])

    def test_consent_redirect_does_not_match_requested_search(self):
        self.assert_failure('PAGE_LOAD_TIMEOUT', [navigation(), response(response={'url': 'https://consent.google.com/', 'status': 200}), loaded()])

    def test_browser_replaced_document(self):
        self.assert_failure('POSTCONDITION_UNVERIFIED', [navigation(), response(), loaded()],
                            observed={'url': URL, 'document': 'chrome-error://chromewebdata/', 'ready': 'complete'})

    def test_partial_document_is_unverified(self):
        self.assert_failure('POSTCONDITION_UNVERIFIED', [navigation(), response(), loaded()],
                            observed={'url': URL, 'document': URL, 'ready': 'interactive'})

    def test_exception_page_without_location(self):
        self.assert_failure('POSTCONDITION_UNVERIFIED', [navigation(), response(), loaded()], observed={})
