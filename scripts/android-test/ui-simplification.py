"""Bookmuse 0.8.1 focused UI regression. Dedicated emulator only; private inputs stay private."""
from pathlib import Path
import importlib.util,json,re,time,sys,hashlib,subprocess
from PIL import Image
spec=importlib.util.spec_from_file_location('qa',Path(__file__).with_name('ux-regression.py'))
q=importlib.util.module_from_spec(spec);spec.loader.exec_module(q);u=q.u
q.report=q.repo/'docs/acceptance/reports/UI-SIMPLIFICATION-functional.json'

def visible(label):
 for _ in range(12):
  root=u.root();node=u.find(label,root=root)
  b=list(map(int,re.findall(r'\d+',node.get('bounds','')))) if node is not None else []
  if len(b)==4 and b[2]>b[0] and b[3]>b[1] and b[1]>=0 and b[3]<=q.H-16:
   u.click(label);return
  u.run('shell','input','swipe',str(q.W//2),str(q.H*78//100),str(q.W//2),str(q.H*32//100),'650')
 raise AssertionError('Control not reachable: '+label)

def background(name,expected):
 im=Image.open(u.screenshot(name)).convert('RGB')
 pixel=im.getpixel((8,im.height//2));assert max(abs(a-b) for a,b in zip(pixel,expected))<=4,(name,pixel)
 return '#%02X%02X%02X'%pixel

def open_settings():
 q.top();u.click('Ayarlar');u.wait('Okuma Görünümü');q.sync_dimensions()

def click_node(node):
 left,top,right,bottom=map(int,re.findall(r'\d+',node.get('bounds')))
 u.run('shell','input','tap',str((left+right)//2),str((top+bottom)//2))

def rename_book(old,new):
 u.click(old+', kitap menüsü');u.click('Adı düzenle');node=u.wait('Yeni kitap adı');assert node.get('text')==old
 # Capture the save target before typing. On this emulator a hardware-keyboard
 # edit reports mInputShown=true with no visible IME; Back would close the dialog.
 # UIAutomator can also wait indefinitely on its text/caret updates after input.
 import xml.etree.ElementTree as ET
 root=ET.fromstring(u.run('shell','cat','/data/local/tmp/kitap-ui.xml'));target=u.find('Adı kaydet',root=root);assert target is not None
 q.replace(new);click_node(target);u.wait(new+', kitap menüsü')

def before_after_upgrade():
 before=json.loads((u.private/'ui-before-update.json').read_text(encoding='utf-8'))
 q.restart()
 with q.db() as c:
  assert c.execute('pragma user_version').fetchone()[0]==7
  for t,rows in before.items():
   now=c.execute('select * from '+t+' order by 1').fetchall()
   assert [list(row) for row in now]==rows,t+' changed during update'
  assert c.execute('pragma foreign_key_check').fetchall()==[]
 q.output('upgrade',{'from':'0.8.0/code15','to':'0.8.1/code16','schema':7,'allRowsIdentical':list(before),'counts':{t:len(rows) for t,rows in before.items()}})

def library():
 q.restart();q.search('UX-80.pdf');u.click('UX-80.pdf, kitap menüsü');root=u.root()
 labels=[n.get('content-desc') for n in root.iter('node') if n.get('clickable')=='true' and n.get('content-desc') in ['Notlar','Altı çizilenler','Favoriye ekle','Favoriden çıkar','Adı düzenle','Kaldır']]
 assert len(labels)==5 and labels[:2]==['Notlar','Altı çizilenler'] and labels[-2:]==['Adı düzenle','Kaldır'],labels
 u.click('Altı çizilenler');u.wait('Kayıt panelini kapat');u.click('Kayıt panelini kapat')
 u.click('UX-80.pdf, kitap menüsü');u.click('Notlar');u.wait('Not panelini kapat');u.click('Not panelini kapat');print('Both saved-data panels opened and closed',flush=True)
 with q.db() as c:old=c.execute('select favorite from documents where id=?',(q.metadata('UX-80.pdf')[0],)).fetchone()[0]
 for target in [not old,bool(old)]:
  u.click('UX-80.pdf, kitap menüsü');u.click('Favoriye ekle' if target else 'Favoriden çıkar');u.wait('Kitaplığım')
 with q.db() as c:assert c.execute('select favorite from documents where id=?',(q.metadata('UX-80.pdf')[0],)).fetchone()[0]==old
 print('Favorite toggle restored',flush=True)
 identity=q.metadata('UX-80.pdf');q.search('UX-');rename_book('UX-80.pdf','UX-Renamed.pdf')
 assert q.metadata('UX-Renamed.pdf')==identity
 rename_book('UX-Renamed.pdf','UX-80.pdf')
 q.output('library',{'menuActions':labels,'notesAndHighlightsReachable':True,'favoriteToggleAndRestore':True,'renamePrefilledAndStableIdPage':True})

def settings():
 q.restart();open_settings();root=u.root()
 for text in ['Kitapları yönet','Yedek ve depolama','Yedekleme ve Depolama','Okuma görünümü ve ekran','Krem']:
  assert u.find(text,root=root) is None,text
 colors={}
 for label in ['Orijinal','Sepya','Gece','Özel']:
  u.click(label);colors[label]=background('ui-settings-'+str(len(colors))+'.png',(245,243,237))
 import xml.etree.ElementTree as ET
 root=u.root();input_node=next((n for n in root.iter('node') if n.get('class')=='android.widget.EditText' and n.get('content-desc')=='Arka plan rengi'),None);night_node=u.find('Gece',root=root);assert input_node is not None and night_node is not None
 click_node(input_node);q.replace(r'\#000000');colors['customBlack']=background('ui-settings-custom-black.png',(245,243,237));click_node(night_node)
 u.run('shell','input','keyevent','4');u.wait('Kitaplığım')
 open_settings();u.click('Gece');visible('Ayarları kaydet');u.wait('Kitaplığım')
 q.restart();open_settings();assert u.find('Gece').get('selected')=='true'
 background('ui-settings-reopened-night.png',(245,243,237));visible('Vazgeç');u.wait('Kitaplığım')
 old=q.metadata('UX-80.pdf');q.open_book('UX-80.pdf');background('ui-reader-night.png',(30,33,36));q.leave_reader();assert q.metadata('UX-80.pdf')==old
 q.open_book('UX-layouts.pdf');background('ui-reader-night-second.png',(30,33,36));q.leave_reader()
 q.output('settings',{'directSingleScreen':True,'removedCategoriesAbsent':True,'themeBackgrounds':colors,'savedNightPersistsWithoutDarkSettings':True,'nightAppliedToTwoBooks':True})

def overview():
 q.restart();old=q.open_book('UX-80.pdf');q.overview();root=u.root()
 for label in ['Yardımcı kontroller','Yardımcı Kontroller','Zoom sıfırla','Büyüt','Küçült']:
  assert u.find(label,root=root) is None,label
 cells=[n for n in root.iter('node') if re.fullmatch(r'Sayfa \d+',n.get('content-desc',''))]
 assert len(set(re.findall(r'\d+',n.get('bounds'))[0] for n in cells))==3
 u.click('Paneli kapat');q.leave_reader();assert q.metadata('UX-80.pdf')==old
 q.open_book('UX-80.pdf');q.jump('UX-80.pdf',60);q.overview();u.click('Yer imi ekle');u.click('Yer imleri');u.wait('Sayfa 60');u.click('Yer imini kaldır');u.click('Paneli kapat')
 q.jump('UX-80.pdf',old[1]+1);q.leave_reader();q.open_book('UX-80.pdf');q.leave_reader();assert q.metadata('UX-80.pdf')==old
 q.output('overview',{'helperControlsAbsent':True,'threeColumns':True,'closingPreservesPosition':True,'numberJumpAndBookmarkToggle':True,'reopenRestoredPage':True})

def alert_choice(label):
 def key(value):return value.casefold().replace('ı','i').replace('\u0307','')
 root=u.root();actual=next((n.get('text') for n in root.iter('node') if key(n.get('text',''))==key(label)),None)
 assert actual is not None,label
 u.click(actual)

def removal():
 q.restart();before=q.counts();already_present=q.metadata('UX-Remove-Only-Test.pdf') is not None
 if not already_present:q.import_pdf('UX-Remove-Only-Test.pdf')
 q.search('UX-Remove-Only-Test.pdf');identity=q.metadata('UX-Remove-Only-Test.pdf');assert identity
 u.click('UX-Remove-Only-Test.pdf, kitap menüsü');u.click('Kaldır');u.wait('Kitaplıktan kaldır');alert_choice('Vazgeç');assert q.metadata('UX-Remove-Only-Test.pdf')==identity
 u.click('UX-Remove-Only-Test.pdf, kitap menüsü');u.click('Kaldır');u.wait('Kitaplıktan kaldır');alert_choice('Kaldır');u.wait('Kitaplığım');assert q.metadata('UX-Remove-Only-Test.pdf') is None
 assert u.run('shell','test','-f','/sdcard/Download/kitap-tests/UX-Remove-Only-Test.pdf')==''
 after=q.counts();assert after['documents']==before['documents']-int(already_present);assert all(before[t]==after[t] for t in ['annotations','notes','note_drafts','bookmarks'])
 q.output('removal',{'syntheticFixtureOnly':True,'cancelPreservesBook':True,'confirmRemovesChosenBook':True,'externalOriginalRemains':True,'existingDataCountsUnchanged':True})


def responsive():
 results=[]
 try:
  for width,height,scale in [(360,800,1),(800,360,2)]:
   u.run('shell','wm','size',f'{width}x{height}');u.run('shell','settings','put','system','font_scale',str(scale));q.restart();q.search('UX-80.pdf')
   for _ in range(8):
    node=u.find('UX-80.pdf, kitap menüsü')
    b=list(map(int,re.findall(r'\d+',node.get('bounds','')))) if node is not None else []
    if len(b)==4 and 0<b[1]<b[3]<q.H-16:break
    u.run('shell','input','swipe',str(q.W//2),str(q.H*80//100),str(q.W//2),str(q.H*25//100),'650')
   u.click('UX-80.pdf, kitap menüsü');root=u.root()
   assert u.find('Notlar',root=root) is not None
   menu_x=None
   for _ in range(8):
    node=u.find('Kaldır');b=list(map(int,re.findall(r'\d+',node.get('bounds','')))) if node is not None else []
    if len(b)==4 and b[2]>b[0] and 0<b[1]<b[3]<q.H-16:break
    root=u.root();target=next(n for n in root.iter('node') if n.get('class')=='android.widget.ScrollView')
    left,top,right,bottom=map(int,re.findall(r'\d+',target.get('bounds')));menu_x=(left+right)//2
    u.run('shell','input','swipe',str(menu_x),str(bottom-16),str(menu_x),str(top+16),'600')
   assert len(b)==4 and 0<b[1]<b[3]<q.H-16,'Last menu item outside window'
   u.screenshot(f'ui-menu-{width}x{height}-font{scale}.png')
   u.run('shell','input','keyevent','4');q.restart();open_settings();background(f'ui-settings-{width}-font{scale}.png',(245,243,237));visible('Vazgeç');u.wait('Kitaplığım')
   results.append({'width':width,'height':height,'fontScale':scale,'menuLastActionReachable':True,'directLightSettingsReachable':True})
 finally:
  u.run('shell','settings','put','system','font_scale','1.0');u.run('shell','wm','size','800x1200');q.restart()
 q.output('responsive',results)



def custom_dark_settings():
 q.restart();open_settings();u.click('Özel');root=u.root()
 input_node=next(n for n in root.iter('node') if n.get('class')=='android.widget.EditText' and n.get('content-desc')=='Arka plan rengi')
 night_node=u.find('Gece',root=root);assert night_node is not None
 click_node(input_node);q.replace(r'\#000000');color=background('ui-settings-custom-black-confirmed.png',(245,243,237));click_node(night_node);visible('Vazgeç');u.wait('Kitaplığım')
 q.output('customDarkSettings',{'settingsBackground':color,'screenshot':'private/ui-settings-custom-black-confirmed.png','cancelPreservesSavedTheme':True})

def final_audit():
 before=json.loads((u.private/'ui-before-update.json').read_text(encoding='utf-8'));q.restart()
 with q.db() as c:
  for t in ['documents','annotations','annotation_ranges','notes','note_drafts','bookmarks','book_preferences']:
   now=c.execute('select * from '+t+' order by 1').fetchall();assert [list(row) for row in now]==before[t],t
  old_positions={r[0]:r[1] for r in before['reading_positions']}
  assert dict(c.execute('select document_id,page_index from reading_positions'))==old_positions
  expected=dict(c.execute('select filename,fingerprint from documents'));assert len(expected)==15
  assert c.execute('pragma quick_check').fetchone()[0]=='ok' and c.execute('pragma foreign_key_check').fetchall()==[]
 import shlex
 lines=u.run('shell','su','0','sha256sum',*[shlex.quote('/data/data/com.kitap/files/books/'+name) for name in expected]).splitlines()
 actual={Path(line.split(None,1)[1].strip()).name:line.split()[0] for line in lines}
 assert all(actual.get(name)==digest for name,digest in expected.items())
 path=u.run('shell','pm','path','com.kitap').strip().removeprefix('package:');installed=u.run('shell','sha256sum',path).split()[0]
 artifact=hashlib.sha256((q.repo/'artifacts/ui-simplification/kitap-uisimplification-universal.apk').read_bytes()).hexdigest();assert installed==artifact
 log=u.run('logcat','-d','-b','crash');assert '>>> com.kitap <<<' not in log and 'Process: com.kitap,' not in log
 q.output('finalAudit',{'originalRowsAndPageIndicesPreserved':True,'pdfCopiesFingerprintVerified':len(expected),'dbIntegrityAndForeignKeys':True,'installedApkMatchesArtifact':installed,'appCrashInAvailableBuffer':False,'osStartupSystemUiDialogExcluded':True})

if __name__=='__main__':
 globals()[sys.argv[1]]()
