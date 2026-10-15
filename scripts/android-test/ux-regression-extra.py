"""Durability and touch integration scenarios for the dedicated release emulator."""
from pathlib import Path
import importlib.util,time,json,re,hashlib,sqlite3,traceback,sys
from PIL import Image,ImageChops
path=Path(__file__).with_name('ux-regression.py');spec=importlib.util.spec_from_file_location('qa',path);q=importlib.util.module_from_spec(spec);spec.loader.exec_module(q);u=q.u
def click_visible(label):
 for _ in range(10):
  node=u.find(label)
  bounds=list(map(int,re.findall(r'\d+',node.get('bounds')))) if node is not None else []
  if len(bounds)==4 and bounds[3]>bounds[1] and 0<=(bounds[1]+bounds[3])//2<q.H-20:u.click(label);return
  u.run('shell','input','swipe',str(q.W//2),str(q.H*75//100),str(q.W//2),str(q.H//3),'650')
 raise AssertionError('Control not reachable')
def favorite(enabled):
 q.search('UX-80.pdf');u.click('UX-80.pdf, kitap menüsü');label='Favoriye ekle' if enabled else 'Favoriden çıkar'
 if u.find(label) is not None:u.click(label)
 else:u.run('shell','input','keyevent','4')
def backup_panel():
 u.click('Ayarlar');u.click('Yedek ve depolama');u.wait('Yedek oluştur')
def backup():
 q.restart();favorite(True);backup_panel();u.click('Yedek oluştur');root=u.root();edits=[n for n in root.iter('node') if n.get('class')=='android.widget.EditText'];assert edits
 x1,y1,x2,y2=map(int,re.findall(r'\d+',edits[0].get('bounds')));u.run('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));q.replace('UX-final-backup.json');u.click('SAVE');
 if u.find('REPLACE') is not None:u.click('REPLACE')
 if u.find('Replace') is not None:u.click('Replace')
 u.wait('Yedek dosyası kaydedildi. PDF dosyalarını ayrıca sakla.',timeout=60);u.click('Yedek panelini kapat');u.click('Kitaplık');favorite(False)
 baseline=q.counts();backup_panel()
 for policy,expected in [('Yerel kayıtları koru, yenileri ekle',0),('Farklı kayıtlarda yedeği kullan',1)]:
  u.click('Yedeği seç ve önizle');q.picker_search('UX-final-backup.json');u.wait('Önizleme hazır. Henüz hiçbir kayıt değiştirilmedi.',timeout=60);u.click(policy);u.wait('Yedek oluştur',timeout=60)
  with q.db() as c:assert c.execute('SELECT favorite FROM documents WHERE id=?',(q.metadata('UX-80.pdf')[0],)).fetchone()[0]==expected
 after=q.counts()
 for table in ['documents','annotations','notes','note_drafts','bookmarks']:assert baseline[table]==after[table]
 u.click('Yedek panelini kapat');q.output('backupRestore',{'createdV2':True,'localConflictKeepsLocal':True,'backupConflictRestoresFavorite':True,'recordCountsUnchanged':True})
def reader_settings(double,orientation):
 u.click('Ayarlar');u.click('Okuma görünümü ve ekran');click_visible('Yatay tablette çift sayfa')
 # The previous click toggled it; restore it when that differs from the requested value.
 state=next(n for n in u.root().iter('node') if n.get('checkable')=='true' and n.get('content-desc')=='Yatay tablette çift sayfa').get('checked')=='true'
 if state!=double:u.click('Yatay tablette çift sayfa')
 u.run('shell','input','swipe',str(q.W//2),str(q.H//3),str(q.W//2),str(q.H*85//100),'650');u.run('shell','input','swipe',str(q.W//2),str(q.H//3),str(q.W//2),str(q.H*85//100),'650')
 click_visible({'auto':'Otomatik yön','portrait':'Dikey yön','landscape':'Yatay yön'}[orientation]);click_visible('Ayarları kaydet');u.click('Kitaplık')
def pair():
 q.restart()
 with q.db() as c:
  row=c.execute("SELECT value FROM app_preferences WHERE key='readerDefaults'").fetchone();old=json.loads(row[0]) if row else {}
 reader_settings(True,'landscape')
 try:
  item=q.open_book('UX-80.pdf');root=u.root();assert len([n for n in root.iter('node') if 'PDF sayfası ' in n.get('content-desc','')])==2
  before=u.screenshot('ux6-spread.png');q.touch(q.W*.25,q.H*.4,q.W*.75,q.H*.6,q.W*.15,q.H*.3,q.W*.85,q.H*.7,800);u.screenshot('ux6-spread-zoom.png');u.run('shell','input','keyevent','4');time.sleep(.5);after=u.screenshot('ux6-spread-reset.png')
  im1=Image.open(before).convert('RGB');im2=Image.open(after).convert('RGB');assert im1.size==im2.size
  difference=ImageChops.difference(im1.crop((0,40,im1.width,im1.height-40)),im2.crop((0,40,im2.width,im2.height-40)))
  changed=sum(1 for c in difference.getdata() if max(c)>16);ratio=changed/(difference.width*difference.height);assert ratio<.015,ratio
  q.touch('double',q.W//4,q.H//2);time.sleep(.5);u.screenshot('ux6-spread-double-tap.png');u.run('shell','input','keyevent','4');time.sleep(.5);assert u.find('Kitaplığım') is None
  q.leave_reader();assert q.metadata('UX-80.pdf')[1]==item[1]
 finally:
  q.restart();reader_settings(bool(old.get('doublePage',False)),old.get('orientation','auto'))
 q.output('spread',{'twoVisiblePanes':True,'crossPanePinchAndBackResetBoth':True,'pixelDifferenceAfterResetRatio':ratio,'pagePreserved':True})
def draft():
 q.restart();q.open_book('UX-80.pdf');u.run('shell','input','tap',str(q.W//2),str(q.H//2));u.wait('Sayfaya not ekle');u.click('Sayfaya not ekle');u.wait('Not metni');u.click('Not metni');q.replace('UX6_draft_recovery');u.wait('Taslak kaydedildi.',timeout=45)
 q.restart();q.search('UX-80.pdf');u.click('UX-80.pdf, kitap menüsü');u.click('Notlar');u.wait('Taslağı aç');u.click('Taslağı aç');root=u.root();assert any('UX6_draft_recovery' in n.get('text','') for n in root.iter('node'));u.click('Klavyeyi kapat');u.click('Notu kaydet');u.wait('Not panelini kapat');u.click('Not panelini kapat')
 q.output('draftRecovery',{'lastCommittedDraftSurvivedForceStop':True,'reopenedFromBookNotes':True,'explicitSaveSucceeded':True})
def pinch():
 q.restart();item=q.open_book('UX-80.pdf');w=q.W;h=q.H
 a=[w*.225,h/3,w*.775,h*2/3];b=[w*.1875,h*.30833,w*.8125,h*.69167]
 q.touch(*a,*b,800);q.touch(*b,*a,800);time.sleep(.5);assert u.find('Paneli kapat') is None
 q.touch(*a,w*.375,h*.425,w*.625,h*.575,800);u.wait('Paneli kapat',timeout=30);assert len(set(int(re.findall(r'\d+',n.get('bounds'))[0]) for n in u.root().iter('node') if re.fullmatch(r'Sayfa \d+',n.get('content-desc',''))))==3
 u.click('Paneli kapat');q.leave_reader();assert q.metadata('UX-80.pdf')[1]==item[1]
 q.output('overPinch',{'reachingFitDoesNotOpen':True,'additionalPinchOpensThreeColumns':True,'closingPreservesPage':True})

def mark():
 q.restart();q.open_book('UX-80.pdf');q.jump('UX-80.pdf',45);before=q.counts();x=q.W//2;y=round(q.H*(.125 if q.W>q.H else .15))
 u.run('shell','input','swipe',str(x),str(y),str(x),str(y),'1100');u.wait('Sarıyla işaretle',timeout=45);u.click('Sarıyla işaretle');u.wait('İşaret kaydedildi.')
 y+=round(q.H*.028);u.run('shell','input','swipe',str(x),str(y),str(x),str(y),'1100');u.wait('Pasaja not ekle',timeout=45);u.click('Pasaja not ekle');u.wait('Not metni');u.click('Not metni');q.replace('UX6_passage_note');u.click('Klavyeyi kapat');u.click('Notu kaydet');time.sleep(1);q.leave_reader();q.restart();q.open_book('UX-80.pdf');u.screenshot('ux6-yellow-final.png')
 after=q.counts();assert after['annotations']==before['annotations']+2;assert after['notes']==before['notes']+1
 with q.db() as c:
  assert c.execute("SELECT COUNT(*) FROM notes n JOIN annotations a ON a.id=n.annotation_id WHERE n.body='UX6_passage_note' AND a.kind='highlight' AND a.color='#FFEB3B'").fetchone()[0]>=1
 q.leave_reader();q.output('yellowAndPassage',{'newYellowHighlightCommitted':True,'passageAnnotationAndNoteCommitted':True,'reopenedOnSamePage':q.metadata('UX-80.pdf')[1]==44,'newAnnotations':2,'newNotes':1})

def wipe():
 import subprocess
 q.restart();q.open_book('UX-80.pdf');q.jump('UX-80.pdf',45);time.sleep(2)
 def centroid(name):
  im=Image.open(u.screenshot(name)).convert('RGB');points=[index%im.width for index,c in enumerate(im.getdata()) if c[0]>c[1] and c[1]>c[2]+20 and c[0]>75 and c[2]<85];assert points,'No saved yellow highlight visible';return sum(points)/len(points)
 base=centroid('ux6-wipe-before.png');x=q.W//2;y=q.H*3//5
 proc=subprocess.Popen([str(u.adb),'-s',u.serial,'shell','CLASSPATH=/data/local/tmp/kitap-touch.jar','app_process','/system/bin','KitapTouch','drag',str(x),str(y),str(x+250),str(y),'800','5000','cancel'],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
 try:
  assert b'Drag hold started' in proc.stdout.readline();held=centroid('ux6-wipe-held.png');assert 240<held-base<260,(base,held)
 finally:
  _,err=proc.communicate(timeout=30);assert proc.returncode==0,err
 time.sleep(.5);after=centroid('ux6-wipe-cancel.png');assert abs(after-base)<2; q.leave_reader();assert q.metadata('UX-80.pdf')[1]==44
 q.output('wipe',{'savedHighlightTracksFingerPixels':round(held-base,2),'cancelRestoresPixelsAndSavedPage':True})

def reading_memory():
 q.restart();q.open_book('UX-80.pdf');time.sleep(1);values={}
 def sample(label):
  raw=u.run('shell','dumpsys','meminfo','com.kitap');(u.private/f'ux6-meminfo-{label}.txt').write_text(raw,encoding='utf-8');match=re.search(r'TOTAL PSS:\s*([\d,]+)',raw);assert match;values[label]=int(match.group(1).replace(',',''))
 sample('fit');q.touch(q.W*.4,q.H*.45,q.W*.6,q.H*.55,q.W*.25,q.H*.3,q.W*.75,q.H*.7,800);time.sleep(1);sample('zoom');q.overview();time.sleep(1);sample('overview');q.leave_reader();q.output('readingMemorySnapshots',{'pssKiB':values,'scope':'Three emulator point samples, not typical/peak/long-session or physical acceptance'})

def audit():
 q.restart();package=u.run('shell','pm','path','com.kitap').strip().removeprefix('package:');assert package.startswith('/data/app/') and package.endswith('/base.apk')
 installed=u.run('shell','sha256sum',package).split()[0];expected=hashlib.sha256((q.repo/'artifacts/ux-mvp-6/kitap-uxmvp6-universal.apk').read_bytes()).hexdigest();assert installed==expected
 with sqlite3.connect(u.private/'ux1-after.db') as old,q.db() as now:
  for row in old.execute('SELECT id,title,body,scope,annotation_id FROM notes'):
   same=now.execute('SELECT title,body,scope,annotation_id FROM notes WHERE id=?',(row[0],)).fetchone();assert same==row[1:]
  for row in old.execute('SELECT id,quote,page_index,created_at FROM annotations'):
   same=now.execute('SELECT quote,page_index,created_at FROM annotations WHERE id=?',(row[0],)).fetchone();assert same==row[1:]
  assert now.execute("SELECT COUNT(*) FROM annotations WHERE kind IN ('underline','highlight') AND (kind!='highlight' OR color!='#FFEB3B')").fetchone()[0]==0
 crash=u.run('logcat','-b','crash','-d','-v','brief');(u.private/'ux6-crash-buffer.txt').write_text(crash);assert 'Process: com.kitap,' not in crash and '>>> com.kitap <<<' not in crash
 events=u.run('logcat','-b','events','-d','-v','brief');(u.private/'ux6-events-buffer.txt').write_text(events,encoding='utf-8');anrs=[line for line in events.splitlines() if 'am_anr' in line and re.search(r'com\.kitap(?:,|\s|\])',line)];assert not anrs
 data={'installedSha256MatchesFinalArtifact':True,'sha256':installed,'oldUserNotesAndAnchorsPreserved':True,'textStylesYellow':True,'applicationCrashInCurrentBootBuffer':False,'applicationAnrInCurrentBootEvents':False,'counts':q.counts(),'sdk':u.run('shell','getprop','ro.build.version.sdk').strip(),'abi':u.run('shell','getprop','ro.product.cpu.abi').strip(),'memTotalKiB':int(re.search(r'MemTotal:\s+(\d+)',u.run('shell','cat','/proc/meminfo')).group(1))}
 q.output('finalAudit',data);q.search('UX-');u.screenshot('ux6-library-final-synthetic.png')
if __name__=='__main__':
 for name in sys.argv[1:]:
  try:globals()[name]()
  except Exception:
   (u.private/f'ux6-{name}-failure.txt').write_text(traceback.format_exc(),encoding='utf-8');q.output(name,{'passed':False,'details':'Private diagnostic retained'});raise
