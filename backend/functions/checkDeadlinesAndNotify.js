const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.checkDeadlinesAndNotify = functions.pubsub.schedule('every 24 hours').onRun(async (context) => {
  const db = admin.firestore();
  const messaging = admin.messaging();

  const now = new Date();
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);

  // Find tasks due within the next 24 hours
  const tasksSnapshot = await db.collection('chats')
    .where('deadline', '<=', tomorrow)
    .where('deadline', '>=', now)
    .get();

  const sendPromises = [];

  tasksSnapshot.forEach((doc) => {
    const task = doc.data();
    const completions = task.completions || {};

    for (const [userId, isCompleted] of Object.entries(completions)) {
      if (!isCompleted) {
        const promise = db.collection('users').doc(userId).get().then(async (userDoc) => {
          if (!userDoc.exists) return;
          const fcmToken = userDoc.data().fcmToken;
          if (fcmToken) {
            await messaging.send({
              token: fcmToken,
              notification: {
                title: `⚠️ Urgent: ${task.priority} Priority Task Due Tomorrow!`,
                body: `"${task.title}" is closing soon. Tap to complete.`
              }
            });
          }
        });
        sendPromises.push(promise);
      }
    }
  });

  await Promise.all(sendPromises);
  return null;
});
