import assert from 'node:assert/strict';
import { app } from './main';
import http from 'node:http';

async function runTests() {
  const server = http.createServer(app);
  await new Promise<void>((resolve) => server.listen(0, resolve));
  const port = (server.address() as any).port;
  const baseUrl = `http://127.0.0.1:${port}`;

  try {
    console.log('Testing GET /api/v1/classes/class-10a/discussions...');
    const getRes = await fetch(`${baseUrl}/api/v1/classes/class-10a/discussions`);
    assert.equal(getRes.status, 200);
    const getData = await getRes.json();
    assert.equal(getData.success, true);
    assert.ok(Array.isArray(getData.data.discussions));
    assert.ok(getData.data.discussions.length >= 3);
    console.log(`✓ Fetched ${getData.data.discussions.length} classroom discussions.`);

    const firstDisc = getData.data.discussions[0];

    console.log(`Testing GET /api/v1/discussions/${firstDisc.id}/replies...`);
    const repliesRes = await fetch(`${baseUrl}/api/v1/discussions/${firstDisc.id}/replies`);
    assert.equal(repliesRes.status, 200);
    const repliesData = await repliesRes.json();
    assert.equal(repliesData.success, true);
    console.log(`✓ Fetched ${repliesData.data.replies.length} replies.`);

    console.log('Testing POST /api/v1/classes/class-10a/discussions (Post a Doubt)...');
    const postRes = await fetch(`${baseUrl}/api/v1/classes/class-10a/discussions`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'x-student-name': 'Arjun Mehta' },
      body: JSON.stringify({
        subject_name: 'Chemistry',
        topic_title: 'Balancing Redox Reaction in Acidic Medium',
        content: 'How do we balance H+ ions and H2O molecules in half reaction method?',
      }),
    });
    assert.equal(postRes.status, 201);
    const postData = await postRes.json();
    assert.equal(postData.success, true);
    const newId = postData.data.discussion.id;
    console.log(`✓ Created discussion with ID: ${newId}`);

    console.log(`Testing POST /api/v1/discussions/${newId}/replies (Reply to Doubt)...`);
    const replyRes = await fetch(`${baseUrl}/api/v1/discussions/${newId}/replies`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'x-student-name': 'Pooja K.' },
      body: JSON.stringify({
        content: 'Add H2O to the oxygen-deficient side and 2H+ to the other side for each water molecule added!',
      }),
    });
    assert.equal(replyRes.status, 201);
    const replyData = await replyRes.json();
    assert.equal(replyData.success, true);
    assert.equal(replyData.data.reply.author_name, 'Pooja K.');
    console.log(`✓ Reply posted successfully: "${replyData.data.reply.content}"`);

    console.log('Testing POST /api/v1/auth/student-login (First-time Student Login)...');
    const loginRes = await fetch(`${baseUrl}/api/v1/auth/student-login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        full_name: 'Karthikeya Reddy',
        class_name: 'Class 10',
        section: 'B',
      }),
    });
    assert.equal(loginRes.status, 200);
    const loginData = await loginRes.json();
    assert.equal(loginData.success, true);
    assert.equal(loginData.data.student.class_id, 'class-10b');
    assert.equal(loginData.data.student.class_title, 'Class 10 – Section B');
    assert.equal(loginData.data.student.is_profile_locked, true);
    console.log(`✓ Student logged in and locked: ${loginData.data.student.full_name} (${loginData.data.student.class_title})`);

    const studentId = loginData.data.student.id;

    console.log('Testing Student Self-Edit Rejection (403 Forbidden after onboarding lock)...');
    const studentSelfEditRes = await fetch(`${baseUrl}/api/v1/students/${studentId}/class-section`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json', 'x-user-role': 'STUDENT' },
      body: JSON.stringify({
        full_name: 'Karthikeya Reddy',
        class_name: 'Class 9',
        section: 'A',
      }),
    });
    assert.equal(studentSelfEditRes.status, 403);
    console.log('✓ Student self-update blocked with 403 Forbidden as expected.');

    console.log('Testing Teacher-Authorized Student Details Update (PATCH /api/v1/students/:id/class-section)...');
    const updateRes = await fetch(`${baseUrl}/api/v1/students/${studentId}/class-section`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json', 'x-user-role': 'TEACHER' },
      body: JSON.stringify({
        full_name: 'Karthikeya Reddy',
        class_name: 'Class 9',
        section: 'A',
      }),
    });
    assert.equal(updateRes.status, 200);
    const updateData = await updateRes.json();
    assert.equal(updateData.data.student.class_id, 'class-9a');
    assert.equal(updateData.data.student.class_title, 'Class 9 – Section A');
    console.log(`✓ Teacher updated student class/section to: ${updateData.data.student.class_title}`);

    console.log('Testing strict Class-Section isolation for Notes & Discussions...');
    const notes9aRes = await fetch(`${baseUrl}/api/v1/classes/class-9a/notes`);
    const notes9aData = await notes9aRes.json();
    assert.ok(notes9aData.data.notes.every((n: any) => n.class_id === 'class-9a'));
    console.log(`✓ Verified strict class-section isolation for class-9a notes (${notes9aData.data.notes.length} notes).`);

    console.log('\nAll classroom discussion & student login API tests passed successfully!');
  } finally {
    server.close();
  }
}

runTests().catch((err) => {
  console.error('Test failed:', err);
  process.exit(1);
});

