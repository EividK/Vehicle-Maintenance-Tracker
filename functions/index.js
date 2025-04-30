'use strict';

const functions = require('firebase-functions');
const admin     = require('firebase-admin');

admin.initializeApp();

exports.sendTopicMessage = functions.https.onCall(async (data, context) => {
    // 1) Signed in?
    if (!context.auth) {
      throw new functions.https.HttpsError(
        'unauthenticated',
        'You must be signed in to send announcements.'
      );
    }
    const uid = context.auth.uid;

    // 2) Check Firestore roles/{uid}
    const roleSnap = await admin
      .firestore()
      .collection('roles')
      .doc(uid)
      .get();

    if (!roleSnap.exists || roleSnap.data().role !== 'ADMIN') {
      throw new functions.https.HttpsError(
        'permission-denied',
        'Only admins may send announcements.'
      );
    }

    // 3) Validate inputs
    const topic = data.topic;
    const title = data.title;
    const body  = data.body;
    const extra = data.data || {};

    if (
      typeof topic !== 'string' ||
      typeof title !== 'string' ||
      typeof body  !== 'string'
    ) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'topic, title and body must be strings'
      );
    }

    // 4) Send FCM to topic
    const message = {
      notification: { title, body },
      data: extra,
      topic: topic
    };

    try {
      const resp = await admin.messaging().send(message);
      return { success: true, result: resp };
    } catch (err) {
      console.error('FCM send error:', err);
      throw new functions.https.HttpsError(
        'internal',
        'Failed to send message: ' + err.message
      );
    }
  });
